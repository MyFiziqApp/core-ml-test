package com.myfiziq.sdk.db;

import android.annotation.SuppressLint;
import android.database.DatabaseUtils;
import android.os.Parcel;
import android.text.TextUtils;

import com.myfiziq.sdk.BuildConfig;
import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.enums.MeasurementType;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.helpers.AppWideUnitSystemHelper;
import com.myfiziq.sdk.util.AwsUtils;
import com.myfiziq.sdk.util.GlobalContext;
import com.myfiziq.sdk.util.MiscUtils;
import com.myfiziq.sdk.util.SensorUtils;
import com.myfiziq.sdk.util.TimeFormatUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.TimeZone;

import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 * Model class for an Avatar.
 * <br>
 * Created and Downloaded Avatars will map to this class and can be used for rendering etc.
 */
@Cached
public class ModelAvatar extends Model
{
    private static final int CAPTURE_OVER_TIMEFRAME = 1000;
    private static final int CAPTURE_FRAMES = 4;

    /**
     * This fields will not be uploaded to the remote server when the avatar is serialised and sent for
     * remote processing.
     */
    static final String[] EXCLUDES_PENDING = {"completeddate", "inseam", "hip", "fitness", "thigh", "chest", "waist", "error_id"};

    /**
     * These fields will not be saved to the remote server after it has finished being processed
     * both locally and remotely.
     */
    static final String[] EXCLUDES_COMPLETE = {"updatedAt", "uploadCount"};

    public interface MeshReadyListener
    {
        void onMeshReady();
    }

    @Persistent
    String frontSourceFile = "";

    @Persistent
    String frontInspectResult = "";

    @Persistent
    String sideSourceFile = "";

    @Persistent
    String sideInspectResult = "";

    @Persistent
    double pitch = 0.0;

    @Persistent
    double GravityZ = 0.0;

    @Persistent
    double roll = 0.0;

    @Persistent
    double GravityY = 0.0;

    @Persistent
    double yaw = 0.0;

    @Persistent
    double GravityX = 0.0;

    /**
     * The current status of the avatar.
     */
    @Persistent
    Status Status = com.myfiziq.sdk.db.Status.Captured;

    /**
     * For now, this is the same as the {@link #requestdate}.
     */
    @Persistent
    String updatedAt = "";

    /**
     * The number of images to upload PER direction/side.
     * <p>
     * (i.e. if we want to upload 8 images for the front and 8 for the side, this value is 8)
     */
    @Persistent
    int uploadCount = 0;

    /**
     * The user's gender.
     */
    @Persistent
    Gender gender = Gender.M;

    /**
     * The avatar's height in centimeters.
     */
    @Persistent
    double height = 0.0;

    /**
     * The user's preferred unit of measurement when displaying height.
     */
    @Persistent
    String heightUnit = Centimeters.internalName;

    /**
     * The avatar's weight in kilograms.
     */
    @Persistent
    double weight = 0.0;

    /**
     * The user's preferred unit of measurement when displaying weight.
     */
    @Persistent
    String weightUnit = Kilograms.internalName;

    /**
     * The time that the avatar was taken.
     */
    @Persistent
    String requestdate = "";

    /**
     * The time that the avatar successfully finished uploading.
     */
    @Persistent
    String uploadeddate = "";

    /**
     * The time that the avatar finished processing on the remote server,
     * was downloaded and finished processing on the device.
     */
    @Persistent
    String completeddate = "";

    /**
     * The user's "inseam" measurement.
     */
    @Persistent
    double inseam = 0;

    @Persistent
    double inseam_adj = 0;

    /**
     * The units of measurement to represent the {@link #inseam} measurement.
     */
    @Persistent
    String inseamUnit = Centimeters.internalName;

    /**
     * The user's "hip" measurement.
     */
    @Persistent
    double hip = 0;

    @Persistent
    double hip_adj = 0;

    /**
     * The units of measurement to represent the {@link #hip} measurement.
     */
    @Persistent
    String hipUnit = Centimeters.internalName;

    /**
     * A number to represent the user's level of fitness.
     */
    @Persistent
    double fitness = 0;

    /**
     * The user's "thigh" measurement.
     */
    @Persistent
    double thigh = 0;

    @Persistent
    double thigh_adj = 0;

    /**
     * The units of measurement to represent the {@link #thigh} measurement.
     */
    @Persistent
    String thighUnit = Centimeters.internalName;

    /**
     * The user's "chest" measurement.
     */
    @Persistent
    double chest = 0;

    @Persistent
    double chest_adj = 0;

    /**
     * The units of measurement to represent the {@link #chest} measurement.
     */
    @Persistent
    String chestUnit = Centimeters.internalName;

    /**
     * The user's "waist" measurement.
     */
    @Persistent
    double waist = 0;

    @Persistent
    double waist_adj = 0;

    /**
     * The units of measurement to represent the {@link #waist} measurement.
     */
    @Persistent
    String waistUnit = Centimeters.internalName;

    /**
     * The user's body fat percent.
     */
    @Persistent
    double PercentBodyFat = 0;

    @Persistent
    double PercentBodyFat_adj = 0;

    /**
     * Represents an error that may have occurred when processing this avatar.
     */
    @Persistent
    String error_id = "";

    /**
     * A unique ID to represent this avatar attempt.
     */
    @Persistent(idMap = true, serialize = false)
    String attemptId = "";

    /**
     * The user's skin tone.
     */
    @Persistent
    String skinTone = "";

    @Persistent
    String device = "";

    @Persistent(jsonMap = "MiscData!guest")
    String miscGuest = "";

    @Persistent
    JsonString MiscData = new JsonString();

    @Persistent(serialize = false)
    boolean Uploaded = false;

    @Persistent(serialize = false)
    boolean UserSeen = false;

    public ModelAvatar(Parcel parcelData)
    {
        unparcel(parcelData);
    }

    public static final Creator<ModelAvatar> CREATOR = new
            Creator<ModelAvatar>()
            {
                public ModelAvatar createFromParcel(Parcel parcel)
                {
                    return new ModelAvatar(parcel);
                }

                public ModelAvatar[] newArray(int size)
                {
                    return new ModelAvatar[size];
                }
            };

    public static String getWhere()
    {
       return getWhere("");
    }

    public static String getWhere(String whereClause)
    {
        return getWhere(whereClause, true, false);
    }

    public static String getWhere(String whereClause, boolean bSeenOnly, boolean bNotSeenOnly)
    {
        String selectedGuest = MyFiziq.getInstance().getGuestUser();
        StringBuilder escapedStringBuilder = new StringBuilder(whereClause);

        if (TextUtils.isEmpty(selectedGuest))
        {
            // miscGuest field may be null in the row if the backend hasn't set it.
            if (TextUtils.isEmpty(whereClause))
            {
                escapedStringBuilder.append(" (miscGuest IS NULL OR miscGuest = ");
            }
            else
            {
                escapedStringBuilder.append(" AND (miscGuest IS NULL OR miscGuest = ");
            }
        }
        else
        {
            if (TextUtils.isEmpty(whereClause))
            {
                escapedStringBuilder.append(" (miscGuest = ");
            }
            else
            {
                escapedStringBuilder.append(" AND (miscGuest = ");
            }
        }

        // Escape the guest name to handle any quotation marks that may have been entered
        DatabaseUtils.appendEscapedSQLString(escapedStringBuilder, selectedGuest);
        escapedStringBuilder.append(")");

        if (bSeenOnly)
        {
            escapedStringBuilder.append(" AND (UserSeen = 1)");
        }
        else if (bNotSeenOnly)
        {
            escapedStringBuilder.append(" AND (UserSeen IS NULL OR UserSeen <> 1)");
        }

        return escapedStringBuilder.toString();
    }

    public static String getWhereWithGuestAvatars(String whereClause)
    {
        // Makes it obvious when reading the code that the SQL query will return guest avatars
        // unlike a normal SQL query and that we need to use "ModelAvatar.getWhere..."
        return "";
    }

    @SuppressLint("DefaultLocale")
    public static String getOrderBy(int limit)
    {
        if (limit > 0)
        {
            return String.format("datetime(requestdate) DESC LIMIT %d", limit);
        }
        else
        {
            return "datetime(requestdate) DESC";
        }
    }

    public ModelAvatar()
    {
    }

    @Override
    public void afterReadFromCursor()
    {
        // Handle edge cases....

        // This handles when the backend gives us an avatar with an error code.
        // error_id...
        if (Status == com.myfiziq.sdk.db.Status.Completed)
        {
            if (!TextUtils.isEmpty(error_id))
            {
                Status = com.myfiziq.sdk.db.Status.FailedGeneral;
            }
        }
    }

    public void set(Gender gender, Length height, Weight weight, int zipCount)
    {
        this.gender = gender;

        this.height = height.getValueInCm();
        this.weight = weight.getValueInKg();

        uploadCount = zipCount;
        init();
    }

    public void set(ModelAvatarReq req)
    {
        gender = req.gender;
        height = req.height;
        weight = req.weight;
        Status = com.myfiziq.sdk.db.Status.valueOf(req.status);
        requestdate = req.requestdate;
        completeddate = req.completeddate;
        inseam = req.inseam;
        hip = req.hip;
        fitness = req.fitness;
        thigh = req.thigh;
        chest = req.chest;
        waist = req.waist;
        PercentBodyFat = req.PercentBodyFat;
        error_id = req.error_id;
        if (!TextUtils.isEmpty(req.attemptId))
        {
            attemptId = req.attemptId;
        }

        uploadCount = 0;
        updatedAt = requestdate;
        id = attemptId;
    }

    public void setGuestToCurrent()
    {
        String guestName = MyFiziq.getInstance().getGuestUser();
        if (guestName != null)
        {
            setMiscElement("guest", guestName);
            miscGuest = guestName;
        }
    }

    private void init()
    {
        updatedAt = requestdate = TimeFormatUtils.formatDate(new Date(), TimeZone.getDefault(), TimeFormatUtils.PATTERN_ISO8601_2, TimeZone.getTimeZone("UTC"));

        String usernameNumber = AwsUtils.getCognitoUsernameNumber();
        attemptId = MiscUtils.leftPad(usernameNumber, 10, "0") + TimeFormatUtils.getUnixTimestampInUtcWithSeconds();

        id = attemptId;
    }

    public String getMiscElement(String name)
    {
        if (null != MiscData)
        {
            return MiscData.getElement(name);
        }

        return null;
    }

    public void setMiscElement(String name, String value)
    {
        if (null != MiscData)
        {
            MiscData.setElement(name, value);
        }
    }

    public String getGuestName()
    {
        return miscGuest;
    }

    public void setSensorValues(double parYaw, double parPitch, double parRoll, double parX, double parY, double parZ)
    {
        yaw = parYaw;
        pitch = parPitch;
        roll = parRoll;
        GravityX = parX;
        GravityY = parY;
        GravityZ = parZ;
    }

    public void setSensorValues(SensorUtils sensorUtils)
    {
        yaw = sensorUtils.getYaw();
        roll = sensorUtils.getRoll();
        pitch = sensorUtils.getPitch();
        GravityX = sensorUtils.getX();
        GravityY = sensorUtils.getY();
        GravityZ = sensorUtils.getZ();
    }

    public void setSensorValues(ModelAvatarSource source)
    {
        yaw = source.yaw;
        roll = source.roll;
        pitch = source.pitch;
        GravityX = source.GravityX;
        GravityY = source.GravityY;
        GravityZ = source.GravityZ;
    }

    public boolean isCompleted()
    {
        return (com.myfiziq.sdk.db.Status.Completed == Status);
    }

    public boolean isPending()
    {
        switch (Status)
        {
            case Completed:
            case FailedGeneral:
            case FailedTimeout:
            case FailedNoInternet:
            case FailedServerErr:
                return false;
        }

        return true;
    }

    public boolean isFailed()
    {
        switch (Status)
        {
            case FailedGeneral:
            case FailedTimeout:
            case FailedNoInternet:
            case FailedServerErr:
                return true;
        }

        return false;
    }

    /**
     * Gets the Unix timestamp that the avatar was taken based on the current timezone.
     */
    public long getRequestTime()
    {
        return getRequestTime(TimeZone.getDefault());
    }

    /**
     * Gets the Unix timestamp that the avatar was taken based on the specified timezone.
     */
    public long getRequestTime(TimeZone timezone)
    {
        Date date = getRequestDate(timezone);

        if (date == null)
        {
            return 0;
        }
        else
        {
            return date.getTime();
        }
    }

    /**
     * Gets the raw requestdate.
     */
    @Nullable
    public String getRequestDateString()
    {
        return requestdate;
    }
    /**
     * Gets a date object that the avatar was taken the avatar was taken based on the current timezone.
     */
    @Nullable
    public Date getRequestDate()
    {
        return getRequestDate(TimeZone.getDefault());
    }

    /**
     * Gets a date object that the avatar was taken the avatar was taken based on the specified timezone.
     */
    @Nullable
    public Date getRequestDate(TimeZone timezone)
    {
        if (!TextUtils.isEmpty(requestdate))
        {
            return TimeFormatUtils.parseDateTime(requestdate, TimeZone.getTimeZone("UTC"), timezone);
        }
        else
        {
            return null;
        }
    }

    /**
     * Gets the time that the avatar successfully finished uploading based on the current timezone.
     */
    public long getUploadedTime()
    {
        return getUploadedTime(TimeZone.getDefault());
    }

    /**
     * Gets the time that the avatar successfully finished uploading based on the specified timezone.
     */
    public long getUploadedTime(TimeZone timeZone)
    {
        if (!TextUtils.isEmpty(uploadeddate))
        {
            Date dt = TimeFormatUtils.parseDateTime(uploadeddate, TimeZone.getTimeZone("UTC"), timeZone);

            if (null != dt)
            {
                return dt.getTime();
            }
        }

        return 0;
    }

    public double getSampleValueInCm(MeasurementType measurementType)
    {
        switch (measurementType)
        {
            case TBF:
                return PercentBodyFat;
            case HEIGHT:
                return height;
            case WEIGHT:
                return weight;
            case CHEST:
                return chest;
            case INSEAM:
                return inseam;
            case HIPS:
                return hip;
            case WAIST:
                return waist;
            case THIGH:
                return thigh;
            default:
                throw new UnsupportedOperationException("Unknown type: " + measurementType);
        }
    }

    // Always get the adjusted value regardless of what feature flags have been set
    public double getAdjustedValueInCmForced(MeasurementType measurementType)
    {
        switch (measurementType)
        {
            case TBF:
                return PercentBodyFat_adj;
            case HEIGHT:
                return height;
            case WEIGHT:
                return weight;
            case CHEST:
                return chest_adj;
            case INSEAM:
                return inseam_adj;
            case HIPS:
                return hip_adj;
            case WAIST:
                return waist_adj;
            case THIGH:
                return thigh_adj;
            default:
                throw new UnsupportedOperationException("Unknown type: " + measurementType);
        }
    }

    public double getAdjustedValueInCm(MeasurementType measurementType)
    {
        switch (measurementType)
        {
            case TBF:
                return PercentBodyFat_adj;
            case HEIGHT:
                return height;
            case WEIGHT:
                return weight;
            case CHEST:
                return chest_adj;
            case INSEAM:
                return inseam_adj;
            case HIPS:
                return hip_adj;
            case WAIST:
                return waist_adj;
            case THIGH:
                return thigh_adj;
            default:
                throw new UnsupportedOperationException("Unknown type: " + measurementType);
        }
    }

    public void setAdjustedValueInCm(MeasurementType measurementType, double adjustedValue)
    {
        switch (measurementType)
        {
            case TBF:
                PercentBodyFat_adj = adjustedValue;
                break;
            case CHEST:
                chest_adj = adjustedValue;
                break;
            case INSEAM:
                inseam_adj = adjustedValue;
                break;
            case HIPS:
                hip_adj = adjustedValue;
                break;
            case WAIST:
                waist_adj = adjustedValue;
                break;
            case THIGH:
                thigh_adj = adjustedValue;
                break;
            default:
                throw new UnsupportedOperationException("Unknown type: " + measurementType);
        }
    }

    public String getFrontInspectResult()
    {
        return frontInspectResult;
    }

    public String getSideInspectResult()
    {
        return sideInspectResult;
    }

    public String getFrontSourceFile()
    {
        return frontSourceFile;
    }

    public String getSideSourceFile()
    {
        return sideSourceFile;
    }

    public String getAttemptId()
    {
        return attemptId;
    }

    public com.myfiziq.sdk.db.Status getStatus()
    {
        return Status;
    }

    public double getPitch()
    {
        return pitch;
    }

    public double getGravityZ()
    {
        return GravityZ;
    }

    public double getRoll()
    {
        return roll;
    }

    public double getGravityY()
    {
        return GravityY;
    }

    public double getYaw()
    {
        return yaw;
    }

    public double getGravityX()
    {
        return GravityX;
    }

    public void setFrontInspectResult(String frontInspectResult)
    {
        this.frontInspectResult = frontInspectResult;
    }

    public void setSideInspectResult(String sideInspectResult)
    {
        this.sideInspectResult = sideInspectResult;
    }

    public void setAttemptId(String attemptId)
    {
        this.attemptId = attemptId;
    }

    public void setPitch(double pitch)
    {
        this.pitch = pitch;
    }

    public void setGravityZ(double gravityZ)
    {
        GravityZ = gravityZ;
    }

    public void setRoll(double roll)
    {
        this.roll = roll;
    }

    public void setGravityY(double gravityY)
    {
        GravityY = gravityY;
    }

    public void setYaw(double yaw)
    {
        this.yaw = yaw;
    }

    public void setGravityX(double gravityX)
    {
        GravityX = gravityX;
    }

    public void setStatus(Status newStatus)
    {
        Status = newStatus;
        save("Status");
    }

    public void setSeen(boolean bSeen)
    {
        UserSeen = bSeen;
        save("UserSeen");
    }

    public boolean updateStatus()
    {
        if (Status == com.myfiziq.sdk.db.Status.Pending && !TextUtils.isEmpty(error_id))
        {
            if (Integer.valueOf(error_id) == 0)
            {
                Status = com.myfiziq.sdk.db.Status.Completed;
                completeddate = TimeFormatUtils.formatDate(new Date(), TimeZone.getDefault(), TimeFormatUtils.PATTERN_ISO8601_2, TimeZone.getTimeZone("UTC"));

                save("Status", "completeddate");

                return true;
            }
        }
        return false;
    }

    public void setErrorStatus(ModelAvatarBatchList avatarList)
    {
        if (avatarList.isError())
        {
            try
            {
                int errorCode = Integer.decode(avatarList.body.error_id);
                if (0 == errorCode)
                {
                    Timber.e("Avatar failed. No internet");
                    setStatus(com.myfiziq.sdk.db.Status.FailedNoInternet);
                }
                else if (errorCode >= 400 && errorCode <= 599)
                {
                    Timber.e("Avatar failed. Received error code: %s", errorCode);
                    setStatus(com.myfiziq.sdk.db.Status.FailedServerErr);
                }
                else
                {
                    Timber.e("Avatar failed. Unknown error.");
                    setStatus(com.myfiziq.sdk.db.Status.FailedGeneral);
                }
            }
            catch (Throwable t)
            {
                Timber.e("Avatar failed. Exception occurred.");
                setStatus(com.myfiziq.sdk.db.Status.FailedGeneral);
                Timber.e(t);
            }
        }
        else
        {
            Timber.e("Avatar failed");
            setStatus(com.myfiziq.sdk.db.Status.FailedGeneral);
        }
    }

    /**
     * Returns the number of frames/images we should capture when creating an avatar.
     */
    public static int getCaptureFrames()
    {
        if (BuildConfig.DEBUG)
        {
            return ModelSetting.getSetting(ModelSetting.Setting.DEBUG_POSEFRAMES, CAPTURE_FRAMES);
        }
        else
        {
            return CAPTURE_FRAMES;
        }
    }

    /**
     * Returns the time span we should take frames/images when creating an avatar.
     */
    public static int getCaptureOverTimeFrame()
    {
        return CAPTURE_OVER_TIMEFRAME;
    }

    public int getUploadCount()
    {
        return uploadCount;
    }

    public void setUploadCount(int uploadCount)
    {
        this.uploadCount = uploadCount;
    }

    public Length getHeight()
    {
        return Length.fromCentimeters(AppWideUnitSystemHelper.getAppWideHeightUnit(), height);
    }

    public void setHeight(Length height)
    {
        this.height = height.getValueInCm();
    }

    public Weight getWeight()
    {
        return Weight.fromKilograms(AppWideUnitSystemHelper.getAppWideWeightUnit(), weight);
    }

    public Length getSampleInseam()
    {
        return Length.fromCentimeters(AppWideUnitSystemHelper.getAppWideLengthUnit(), inseam);
    }

    public void setSampleInseam(Length inseam)
    {
        this.inseam = inseam.getValueInCm();
    }

    public Length getAdjustedInseam()
    {
        return Length.fromCentimeters(AppWideUnitSystemHelper.getAppWideLengthUnit(), inseam_adj);
    }

    public void setAdjustedInseam(Length inseamAdjusted)
    {
        this.inseam_adj = inseamAdjusted.getValueInCm();
    }

    public Length getSampleHip()
    {
        return Length.fromCentimeters(AppWideUnitSystemHelper.getAppWideLengthUnit(), hip);
    }

    public void setSampleHip(Length hip)
    {
        this.hip = hip.getValueInCm();
    }

    public Length getAdjustedHip()
    {
        return Length.fromCentimeters(AppWideUnitSystemHelper.getAppWideLengthUnit(), hip_adj);
    }

    public void setAdjustedHip(Length hipAdjusted)
    {
        this.hip_adj = hipAdjusted.getValueInCm();
    }

    public Length getSampleThigh()
    {
        return Length.fromCentimeters(AppWideUnitSystemHelper.getAppWideLengthUnit(), thigh);
    }

    public void setSampleThigh(Length thigh)
    {
        this.thigh = thigh.getValueInCm();
    }

    public Length getAdjustedThigh()
    {
        return Length.fromCentimeters(AppWideUnitSystemHelper.getAppWideLengthUnit(), thigh_adj);
    }

    public void setAdjustedThigh(Length thighAdjusted)
    {
        this.thigh_adj = thighAdjusted.getValueInCm();
    }

    public Length getSampleChest()
    {
        return Length.fromCentimeters(AppWideUnitSystemHelper.getAppWideLengthUnit(), chest);
    }

    public void setSampleChest(Length chest)
    {
        this.chest = chest.getValueInCm();
    }

    public Length getAdjustedChest()
    {
        return Length.fromCentimeters(AppWideUnitSystemHelper.getAppWideLengthUnit(), chest_adj);
    }

    public void setAdjustedChest(Length chestAdjusted)
    {
        this.chest_adj = chestAdjusted.getValueInCm();
    }

    public Length getSampleWaist()
    {
        return Length.fromCentimeters(AppWideUnitSystemHelper.getAppWideLengthUnit(), waist);
    }

    public void setSampleWaist(Length waist)
    {
        this.waist = waist.getValueInCm();
    }

    public Length getAdjustedWaist()
    {
        return Length.fromCentimeters(AppWideUnitSystemHelper.getAppWideLengthUnit(), waist_adj);
    }

    public void setAdjustedWaist(Length waistAdjusted)
    {
        this.waist_adj = waistAdjusted.getValueInCm();
    }

    public double getSamplePercentBodyFat()
    {
        return PercentBodyFat;
    }

    public void setSamplePercentBodyFat(double percentBodyFat)
    {
        this.PercentBodyFat = percentBodyFat;
    }

    public double getAdjustedPercentBodyFat()
    {
        return PercentBodyFat_adj;
    }

    public void setAdjustedPercentBodyFat(double percentBodyFatAdjusted)
    {
        this.PercentBodyFat_adj = percentBodyFatAdjusted;
    }

    public Gender getGender()
    {
        return gender;
    }

    public void set(Gender gender, Length height, Weight weight)
    {
        initId();
        this.gender = gender;
        this.height = height.getValueInCm();
        this.weight = weight.getValueInKg();
    }

    public void set(Gender gender, float height, float weight)
    {
        initId();
        this.gender = gender;
        this.height = height;
        this.weight = weight;
    }

    public SdkResultCode saveToFile()
    {
        String[] excludes;
        switch (Status)
        {
            default:
            case Pending:
                excludes = EXCLUDES_PENDING;
                break;
            case Completed:
                excludes = EXCLUDES_COMPLETE;
                break;
        }
        try
        {
            String fileJsonData = serialize(excludes);
            byte[] fileJsonDataByteArray = fileJsonData.getBytes();
            File file = getOutputsFile();
            FileOutputStream fOut = new FileOutputStream(file);
            fOut.write(fileJsonDataByteArray);
            fOut.close();
            return SdkResultCode.SUCCESS;
        }
        catch (Exception e)
        {
            Timber.e(e, "Cannot save status to file");
            return SdkResultCode.FILESYSTEM_ERROR;
        }
    }

    public String getName(String filename)
    {
        String result = "";
        if (MyFiziq.getInstance().hasTokens())
        {
            String usernameNumber = AwsUtils.getCognitoUsernameNumber();
            result = String.format("%s/%s/%s/%s", MyFiziq.getInstance().getTokenAid(), usernameNumber, getAttemptId(), filename);
        }

        return result;
    }

    public String getNameForZip()
    {
        String result = "";
        if (MyFiziq.getInstance().hasTokens())
        {
            String usernameNumber = AwsUtils.getCognitoUsernameNumber();
            result = String.format("%s/%s/%s.zip", MyFiziq.getInstance().getTokenAid(), usernameNumber, getAttemptId());
        }
        return result;
    }

    public String getOutputsBaseName()
    {
        return "outputs.json";
    }

    public String getOutputsName()
    {
        return getName(getOutputsBaseName());
    }

    public File getOutputsFile()
    {
        String baseDir = GlobalContext.getContext().getFilesDir().getAbsolutePath();
        return new File(baseDir, getAttemptId() + getOutputsBaseName());
    }

    public String getZipFilename(int ix)
    {
        return getAttemptId() + ix + ".zip";
    }

    public File getZipFile(int ix)
    {
        String baseDir = GlobalContext.getContext().getFilesDir().getAbsolutePath();
        return new File(baseDir, getZipFilename(ix));
    }

    /**
     * Set UserSeen flag for all unseen avatars.
     */
    public static void makeAvatarsSeen()
    {
        // Make new avatars "visible"
        for (ModelAvatar avatar : ORMTable.getModelList(ModelAvatar.class, ModelAvatar.getWhere("", false, true), ModelAvatar.getOrderBy(0)))
        {
            avatar.setSeen(true);
        }
    }
}
