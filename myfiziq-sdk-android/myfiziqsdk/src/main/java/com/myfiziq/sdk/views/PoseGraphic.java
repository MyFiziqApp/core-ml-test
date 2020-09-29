package com.myfiziq.sdk.views;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.myfiziq.sdk.db.ModelInspect;
import com.myfiziq.sdk.db.ModelInspectRes;
import com.myfiziq.sdk.db.ModelJoints;
import com.myfiziq.sdk.db.PoseSide;

public class PoseGraphic extends GraphicOverlay.Graphic
{

    private static final float FACE_POSITION_RADIUS = 12.0f;
    private static final float ID_TEXT_SIZE = 30.0f;
    private static final float ID_Y_OFFSET = 80.0f;
    private static final float ID_X_OFFSET = -70.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;

    private final Paint facePositionPaint;
    private final Paint idPaint;
    private final Paint boxPaint;

    private ModelInspect inspectionResults;
    private PoseSide side;

    public PoseGraphic(GraphicOverlay overlay, ModelInspect inspectionResults, PoseSide side)
    {
        super(overlay);

        this.inspectionResults = inspectionResults;
        this.side = side;

        facePositionPaint = new Paint();
        facePositionPaint.setColor(Color.GREEN);

        idPaint = new Paint();
        idPaint.setColor(Color.BLUE);
        idPaint.setTextSize(ID_TEXT_SIZE);

        boxPaint = new Paint();
        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
    }

    /** Draws the face annotations for position on the supplied canvas. */
    @Override
    public void draw(Canvas canvas)
    {
        ModelInspect inspectionResults = this.inspectionResults;

        if (inspectionResults == null || side == null)
        {
            return;
        }

        switch (side)
        {
            case front:
                drawForFront(canvas, inspectionResults);
                break;
            case side:
                drawForSide(canvas, inspectionResults);
                break;
            default:
                throw new UnsupportedOperationException("Unknown side: " + side);
        }
    }

    private void drawForFront(Canvas canvas, ModelInspect inspectionResults)
    {
        ModelJoints joints = inspectionResults.joints;
        ModelInspectRes result = inspectionResults.result;


        // This data point might not actually be the face?
        // It's far away from the actual head
        /*canvas.drawCircle(
                scaleX(joints.FaceX),
                scaleY(joints.FaceY),
                FACE_POSITION_RADIUS,
                getColour(result.Face));*/


        canvas.drawCircle(
                scaleX(joints.HeadX),
                scaleY(joints.HeadY),
                FACE_POSITION_RADIUS,
                getColour(result.Face));



        canvas.drawCircle(
                scaleX(joints.NeckX),
                scaleY(joints.NeckY),
                FACE_POSITION_RADIUS,
                getColour(result.Face));




        canvas.drawCircle(
                scaleX(joints.LeftShoulderX),
                scaleY(joints.LeftShoulderY),
                FACE_POSITION_RADIUS,
                getColour(result.LA));

        canvas.drawCircle(
                scaleX(joints.RightShoulderX),
                scaleY(joints.RightShoulderY),
                FACE_POSITION_RADIUS,
                getColour(result.RA));


        canvas.drawCircle(
                scaleX(joints.LeftElbowX),
                scaleY(joints.LeftElbowY),
                FACE_POSITION_RADIUS,
                getColour(result.LA));


        canvas.drawCircle(
                scaleX(joints.RightElbowX),
                scaleY(joints.RightElbowY),
                FACE_POSITION_RADIUS,
                getColour(result.RA));


        canvas.drawCircle(
                scaleX(joints.LeftWristX),
                scaleY(joints.LeftWristY),
                FACE_POSITION_RADIUS,
                getColour(result.LA));


        canvas.drawCircle(
                scaleX(joints.RightWristX),
                scaleY(joints.RightWristY),
                FACE_POSITION_RADIUS,
                getColour(result.RA));



        canvas.drawCircle(
                scaleX(joints.LeftHipX),
                scaleY(joints.LeftHipY),
                FACE_POSITION_RADIUS,
                getColour(result.LL));

        canvas.drawCircle(
                scaleX(joints.RightHipX),
                scaleY(joints.RightHipY),
                FACE_POSITION_RADIUS,
                getColour(result.RL));


        canvas.drawCircle(
                scaleX(joints.LeftKneeX),
                scaleY(joints.LeftKneeY),
                FACE_POSITION_RADIUS,
                getColour(result.LL));

        canvas.drawCircle(
                scaleX(joints.RightKneeX),
                scaleY(joints.RightKneeY),
                FACE_POSITION_RADIUS,
                getColour(result.RL));

        canvas.drawCircle(
                scaleX(joints.LeftAnkleX),
                scaleY(joints.LeftAnkleY),
                FACE_POSITION_RADIUS,
                getColour(result.LL));

        canvas.drawCircle(
                scaleX(joints.RightAnkleX),
                scaleY(joints.RightAnkleY),
                FACE_POSITION_RADIUS,
                getColour(result.RL));


        canvas.drawLine(scaleX(joints.HeadX), scaleY(joints.HeadY), scaleX(joints.NeckX), scaleY(joints.NeckY), getColour(result.Face));

        canvas.drawLine(scaleX(joints.LeftShoulderX), scaleY(joints.LeftShoulderY), scaleX(joints.NeckX), scaleY(joints.NeckY), getColour(result.Face));
        canvas.drawLine(scaleX(joints.NeckX), scaleY(joints.NeckY), scaleX(joints.RightShoulderX), scaleY(joints.RightShoulderY), getColour(result.Face));

        canvas.drawLine(scaleX(joints.LeftElbowX), scaleY(joints.LeftElbowY), scaleX(joints.LeftWristX), scaleY(joints.LeftWristY), getColour(result.LA));
        canvas.drawLine(scaleX(joints.RightElbowX), scaleY(joints.RightElbowY), scaleX(joints.RightWristX), scaleY(joints.RightWristY), getColour(result.RA));
        canvas.drawLine(scaleX(joints.LeftShoulderX), scaleY(joints.LeftShoulderY), scaleX(joints.LeftElbowX), scaleY(joints.LeftElbowY), getColour(result.LA));
        canvas.drawLine(scaleX(joints.RightShoulderX), scaleY(joints.RightShoulderY), scaleX(joints.RightElbowX), scaleY(joints.RightElbowY), getColour(result.RA));
        canvas.drawLine(scaleX(joints.LeftShoulderX), scaleY(joints.LeftShoulderY), scaleX(joints.LeftHipX), scaleY(joints.LeftHipY), getColour(result.LA));
        canvas.drawLine(scaleX(joints.RightShoulderX), scaleY(joints.RightShoulderY), scaleX(joints.RightHipX), scaleY(joints.RightHipY), getColour(result.RA));
        canvas.drawLine(scaleX(joints.LeftHipX), scaleY(joints.LeftHipY), scaleX(joints.LeftKneeX), scaleY(joints.LeftKneeY), getColour(result.LL));
        canvas.drawLine(scaleX(joints.RightHipX), scaleY(joints.RightHipY), scaleX(joints.RightKneeX), scaleY(joints.RightKneeY), getColour(result.RL));
        canvas.drawLine(scaleX(joints.LeftKneeX), scaleY(joints.LeftKneeY), scaleX(joints.LeftAnkleX), scaleY(joints.LeftAnkleY), getColour(result.LL));
        canvas.drawLine(scaleX(joints.RightKneeX), scaleY(joints.RightKneeY), scaleX(joints.RightAnkleX), scaleY(joints.RightAnkleY), getColour(result.RL));
    }

    private void drawForSide(Canvas canvas, ModelInspect inspectionResults)
    {
        ModelJoints joints = inspectionResults.joints;
        ModelInspectRes result = inspectionResults.result;

        // This data point might not actually be the face?
        // It's far away from the actual head
        //canvas.drawCircle(
        //        scaleX(joints.FaceX),
        //        scaleY(joints.FaceY),
        //        FACE_POSITION_RADIUS,
        //        getColour(result.Face));


        canvas.drawCircle(
                scaleX(joints.HeadX),
                scaleY(joints.HeadY),
                FACE_POSITION_RADIUS,
                getColour(result.Face));



        canvas.drawCircle(
                scaleX(joints.NeckX),
                scaleY(joints.NeckY),
                FACE_POSITION_RADIUS,
                getColour(result.Face));




        canvas.drawCircle(
                scaleX(joints.LeftShoulderX),
                scaleY(joints.LeftShoulderY),
                FACE_POSITION_RADIUS,
                getColour(result.UB));

        canvas.drawCircle(
                scaleX(joints.RightShoulderX),
                scaleY(joints.RightShoulderY),
                FACE_POSITION_RADIUS,
                getColour(result.UB));


        canvas.drawCircle(
                scaleX(joints.LeftElbowX),
                scaleY(joints.LeftElbowY),
                FACE_POSITION_RADIUS,
                getColour(result.UB));


        canvas.drawCircle(
                scaleX(joints.RightElbowX),
                scaleY(joints.RightElbowY),
                FACE_POSITION_RADIUS,
                getColour(result.UB));


        canvas.drawCircle(
                scaleX(joints.LeftWristX),
                scaleY(joints.LeftWristY),
                FACE_POSITION_RADIUS,
                getColour(result.UB));


        canvas.drawCircle(
                scaleX(joints.RightWristX),
                scaleY(joints.RightWristY),
                FACE_POSITION_RADIUS,
                getColour(result.UB));



        canvas.drawCircle(
                scaleX(joints.LeftHipX),
                scaleY(joints.LeftHipY),
                FACE_POSITION_RADIUS,
                getColour(result.LB));

        canvas.drawCircle(
                scaleX(joints.RightHipX),
                scaleY(joints.RightHipY),
                FACE_POSITION_RADIUS,
                getColour(result.LB));


        canvas.drawCircle(
                scaleX(joints.LeftKneeX),
                scaleY(joints.LeftKneeY),
                FACE_POSITION_RADIUS,
                getColour(result.LB));

        canvas.drawCircle(
                scaleX(joints.RightKneeX),
                scaleY(joints.RightKneeY),
                FACE_POSITION_RADIUS,
                getColour(result.LB));

        canvas.drawCircle(
                scaleX(joints.LeftAnkleX),
                scaleY(joints.LeftAnkleY),
                FACE_POSITION_RADIUS,
                getColour(result.LB));

        canvas.drawCircle(
                scaleX(joints.RightAnkleX),
                scaleY(joints.RightAnkleY),
                FACE_POSITION_RADIUS,
                getColour(result.LB));


        canvas.drawLine(scaleX(joints.HeadX), scaleY(joints.HeadY), scaleX(joints.NeckX), scaleY(joints.NeckY), getColour(result.Face));

        canvas.drawLine(scaleX(joints.LeftShoulderX), scaleY(joints.LeftShoulderY), scaleX(joints.NeckX), scaleY(joints.NeckY), getColour(result.Face));
        canvas.drawLine(scaleX(joints.NeckX), scaleY(joints.NeckY), scaleX(joints.RightShoulderX), scaleY(joints.RightShoulderY), getColour(result.Face));

        canvas.drawLine(scaleX(joints.LeftElbowX), scaleY(joints.LeftElbowY), scaleX(joints.LeftWristX), scaleY(joints.LeftWristY), getColour(result.UB));
        canvas.drawLine(scaleX(joints.RightElbowX), scaleY(joints.RightElbowY), scaleX(joints.RightWristX), scaleY(joints.RightWristY), getColour(result.UB));
        canvas.drawLine(scaleX(joints.LeftShoulderX), scaleY(joints.LeftShoulderY), scaleX(joints.LeftElbowX), scaleY(joints.LeftElbowY), getColour(result.UB));
        canvas.drawLine(scaleX(joints.RightShoulderX), scaleY(joints.RightShoulderY), scaleX(joints.RightElbowX), scaleY(joints.RightElbowY), getColour(result.UB));
        canvas.drawLine(scaleX(joints.LeftShoulderX), scaleY(joints.LeftShoulderY), scaleX(joints.LeftHipX), scaleY(joints.LeftHipY), getColour(result.UB));
        canvas.drawLine(scaleX(joints.RightShoulderX), scaleY(joints.RightShoulderY), scaleX(joints.RightHipX), scaleY(joints.RightHipY), getColour(result.UB));
        canvas.drawLine(scaleX(joints.LeftHipX), scaleY(joints.LeftHipY), scaleX(joints.LeftKneeX), scaleY(joints.LeftKneeY), getColour(result.LB));
        canvas.drawLine(scaleX(joints.RightHipX), scaleY(joints.RightHipY), scaleX(joints.RightKneeX), scaleY(joints.RightKneeY), getColour(result.LB));
        canvas.drawLine(scaleX(joints.LeftKneeX), scaleY(joints.LeftKneeY), scaleX(joints.LeftAnkleX), scaleY(joints.LeftAnkleY), getColour(result.LB));
        canvas.drawLine(scaleX(joints.RightKneeX), scaleY(joints.RightKneeY), scaleX(joints.RightAnkleX), scaleY(joints.RightAnkleY), getColour(result.LB));
    }

    private Paint getColour(int isValid1, int isValid2)
    {
        if (isValid1 == 1 && isValid2 == 1)
        {
            return getColour(1);
        }
        else
        {
            return getColour(0);
        }
    }

    private Paint getColour(int isValid)
    {
        Paint paint = new Paint();

        if (isValid == 1)
        {
            paint.setColor(Color.GREEN);
        }
        else if (isValid == -1)
        {
            paint.setColor(Color.YELLOW);
        }
        else
        {
            paint.setColor(Color.RED);
        }

        paint.setStrokeWidth(BOX_STROKE_WIDTH);

        return paint;
    }
}
