package com.myfiziq.sdk.manager;

import android.annotation.SuppressLint;
import android.text.TextUtils;

import com.google.gson.GsonBuilder;
import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.MyFiziqApiCallbackPayload;
import com.myfiziq.sdk.db.CreateMeshRunnable;
import com.myfiziq.sdk.db.FactoryAvatar;
import com.myfiziq.sdk.db.ModelAppConf;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ModelAvatarDelete;
import com.myfiziq.sdk.db.ModelAvatarDeleteErrorResponse;
import com.myfiziq.sdk.db.ModelAvatarDeleteResponse;
import com.myfiziq.sdk.db.ModelAvatarDeleteResponseBody;
import com.myfiziq.sdk.db.ModelLambdaHeaders;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.util.AwsUtils;

import timber.log.Timber;


// Package private. Should not be exposed to the customer app.
class MyFiziqAvatarDeletionService
{
    /**
     * Deletes an avatar on the server.
     *
     * @param avatar The avatar delete.
     * @param callback A callback indicating whether the operation was successful.
     */
    void deleteAvatar(ModelAvatar avatar, MyFiziqApiCallbackPayload<Boolean> callback)
    {
        MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();
        if (null == avatar)
        {
            Timber.e(new Throwable(), "ModelAvatar is null");
            mgr.postCallback(callback, SdkResultCode.AVATAR_EMPTY_STATUS, "modelAvatar is null", false);
            return;
        }

        ModelAppConf conf = ModelAppConf.getInstance();
        if (null == conf)
        {
            Timber.e(new Throwable(), "ModelAppConf is null");
            mgr.postCallback(callback, SdkResultCode.SDK_EMPTY_CONFIGURATION, "ModelAppConf is null", false);
            return;
        }

        cancelMeshes(avatar);

        String jsonResult = "";

        try
        {
            if (!TextUtils.isEmpty(conf.interface_url))
            {
                MyFiziq mfz = MyFiziq.getInstance();

                //
                // !!!!!!!!! WARNING WARNING WARNING! !!!!!!!!!
                // !!!!!!!!!!!!!! DANGER DANGER !!!!!!!!!!!!!!!
                //
                // DO NOT CHANGE THE BELOW LINE TO ANYTHING EXCEPT FOR
                // "Integer responseCode = new Integer(0);"
                // THE IDE WILL TEMPT YOU. IT WILL TELL YOU IT CAN BE DONE MORE EFFICIENTLY, BUT DON'T BELIEVE IT
                // EVEN TRYING TO DO "Integer responseCode = 0" WILL CAUSE THE JNI LAYER TO BECOME
                // CORRUPTED IN AN UNIMAGINABLE WAY. FUTURE JNI CALLS WILL FAIL THAT HAVE NOTHING
                // TO DO WITH NETWORKING!
                //
                // Note, the responseCode is passed by reference to the C++ code.
                // The HTTP response will appear in this variable after the C++ code has performed the request.
                @SuppressLint("UseValueOf")
                Integer responseCode = new Integer(0);                              // NOSONAR

                String endpoint = conf.interface_url + "/" + conf.env + "/delete";

                jsonResult = mfz.apiPost(
                        "",
                        endpoint,
                        responseCode,
                        FLAG.getFlags(FLAG.FLAG_NOBASE, FLAG.FLAG_DATA, FLAG.FLAG_TOKEN, FLAG.FLAG_RESPONSE),
                        "[\""+avatar.getAttemptId()+"\"]"
                );

                if (jsonResult.contains("success"))
                {
                    avatar.delete();

                    mgr.postCallback(callback, SdkResultCode.SUCCESS, "Successfully deleted avatar", true);
                    return;
                }
            }
            else
            {
                String idToken = AwsUtils.getRawIdToken();

                ModelLambdaHeaders header = new ModelLambdaHeaders(conf.aid, idToken);

                String[] attemptIds = new String[]{avatar.getAttemptId()};

                ModelAvatarDelete payload = new ModelAvatarDelete(header, attemptIds);
                String jsonRequest = payload.serialize();

                jsonResult = S3Helper.invoke("avatardelete", jsonRequest);

                Timber.i("Delete avatar server response: %s", jsonResult);

                boolean couldHandleAvatarDeleteSuccess = handleAvatarDeleteSuccess(avatar, jsonResult);

                if (couldHandleAvatarDeleteSuccess)
                {
                    mgr.postCallback(callback, SdkResultCode.SUCCESS, "Successfully deleted avatar", true);
                    return;
                }

                boolean couldHandleAvatarDeleteFailure = handleAvatarDeleteNoSuchKey(avatar, jsonResult);

                if (couldHandleAvatarDeleteFailure)
                {
                    mgr.postCallback(callback, SdkResultCode.SUCCESS, "Avatar delete failed but was handled", true);
                    return;
                }
            }
        }
        catch (Exception e)
        {
            Timber.e(e, "Could not serialize or transfer avatardelete payload");
            mgr.postCallback(callback, SdkResultCode.EXECUTION_EXCEPTION, "Could not serialize or transfer avatardelete payload", false);
            return;
        }

        Timber.e("Could not handle delete avatar response. All strategies exhausted.");
        mgr.postCallback(callback, SdkResultCode.ERROR, "Could not handle delete avatar response. All strategies exhausted.", false);
    }

    /**
     * Attempts to handle a successful response from the server when deleting an avatar.
     *
     * @param avatar The avatar being deleted.
     * @param jsonResult The "delete avatar" response from the server.
     * @return Whether we could handle the response from the server or not.
     */
    private boolean handleAvatarDeleteSuccess(ModelAvatar avatar, String jsonResult)
    {
        try
        {
            ModelAvatarDeleteResponse resultVO = new GsonBuilder().create().fromJson(jsonResult, ModelAvatarDeleteResponse.class);
            if (resultVO != null && resultVO.getBody() != null)
            {
                ModelAvatarDeleteResponseBody resultVOBody = resultVO.getBody();

                boolean success = (resultVOBody != null && resultVOBody.getSuccess());

                if (success)
                {
                    // Delete avatar locally
                    avatar.delete();

                    Timber.i("Avatar was successfully deleted remotely and locally.");
                }

                return success;
            }
        }
        catch(Exception e)
        {
            Timber.e(e, "Could not deserialise delete avatar response JSON payload.");
        }

        return false;
    }

    /**
     * Attempts to handle a "NoSuchKey" error from the server when deleting an avatar.
     *
     * This usually occurs when the avatar was already deleted on the server (maybe from another device?).
     *
     * @param avatar The avatar being deleted.
     * @param jsonResult The "delete avatar" response from the server.
     * @return Whether we could handle the response from the server or not.
     */
    private boolean handleAvatarDeleteNoSuchKey(ModelAvatar avatar, String jsonResult)
    {
        try
        {
            ModelAvatarDeleteErrorResponse errorResultVO = new GsonBuilder().create().fromJson(jsonResult, ModelAvatarDeleteErrorResponse.class);
            if (errorResultVO != null)
            {
                if (errorResultVO.getErrorType().equals("NoSuchKey"))
                {
                    // Avatar was already deleted on the server (maybe from another device?)
                    // Lets delete it on this device too
                    avatar.delete();

                    Timber.i("Avatar was already deleted on the server or from another device. Deleting locally...");

                    return true;
                }
                else
                {
                    Timber.e(
                            "Received error from server when deleting avatar. Error Type: %s, Error Message: %s",
                            errorResultVO.getErrorType(),
                            errorResultVO.getErrorMessage()
                    );

                    return false;
                }
            }
        }
        catch(Exception e)
        {
            Timber.e(e, "Could not deserialise delete avatar error response JSON payload.");
        }

        return false;
    }

    /**
     * Cancels any meshes that are currently being generated.
     *
     * We do not want a mesh to be completed if the avatar has been deleted.
     */
    private void cancelMeshes(ModelAvatar avatar)
    {
        boolean inQueue = FactoryAvatar.getInstance().isAvatarCurrentlyInQueue(avatar);

        if (inQueue)
        {
            CreateMeshRunnable meshRunnable = FactoryAvatar.getInstance().getExistingQueueItem(avatar);
            if (meshRunnable == null)
            {
                return;
            }

            meshRunnable.cancel();
        }
    }
}
