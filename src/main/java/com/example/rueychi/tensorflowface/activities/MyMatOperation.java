package com.example.rueychi.tensorflowface.activities;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class MyMatOperation {
    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    private static final int FONT_SIZE = 3;  //字體大小
    private static final int THICKNESS = 2;  //綠框線條粗細

    public static void rotate_90n(Mat img, int angle)
    {
        if(angle == 270 || angle == -90){
            // Rotate clockwise 270 degrees
            Core.transpose(img, img);
            Core.flip(img, img, 0);
        }else if(angle == 180 || angle == -180){
            // Rotate clockwise 180 degrees
            Core.flip(img, img, -1);
        }else if(angle == 90 || angle == -270){
            // Rotate clockwise 90 degrees
            Core.transpose(img, img);
            Core.flip(img, img, 1);
        }
    }

    public static Point drawRectangleOnPreview(Mat img, Rect face, boolean front_camera){
        if(front_camera){
            Rect mirroredFace = getMirroredFaceForFrontCamera(img, face);
            Imgproc.rectangle(img, mirroredFace.tl(), mirroredFace.br(), FACE_RECT_COLOR, THICKNESS);
            return mirroredFace.tl();
        } else {
            Imgproc.rectangle(img, face.tl(), face.br(), FACE_RECT_COLOR, THICKNESS);
            return face.tl();
        }
    }

    public static void drawRectangleAndLabelOnPreview(Mat img, Rect face, String label, boolean front_camera){
       Point tl = drawRectangleOnPreview(img, face, front_camera);
        Log.i("BBB",label);
        Imgproc.putText(img, label, tl, Core.FONT_HERSHEY_PLAIN, FONT_SIZE, FACE_RECT_COLOR, THICKNESS);
    }

    public static Rect[] rotateFaces(Mat img, Rect[] faces, int angle){
        Point center = new Point(img.cols()/2, img.rows()/2);
        Mat rotMat = Imgproc.getRotationMatrix2D(center, angle, 1);
        rotMat.convertTo(rotMat, CvType.CV_32FC1);
        float scale = img.cols()/img.rows();
        for(Rect face : faces){
            Mat m = new Mat(3, 1, CvType.CV_32FC1);
            m.put(0,0,face.x);
            m.put(1,0,face.y);
            m.put(2,0,1);
            Mat res = Mat.zeros(2,1,CvType.CV_32FC1);
            Core.gemm(rotMat, m, 1, new Mat(), 0, res, 0);
            face.x = (int)res.get(0,0)[0];
            face.y = (int)res.get(1,0)[0];
            if(angle == 270 || angle == -90){
                face.x = (int)(face.x * scale - face.width);
                face.x = face.x + face.width/4;
                face.y = face.y + face.height/4;
            }else if(angle == 180 || angle == -180){
                face.x = face.x - face.width;
                face.y = face.y - face.height;
            }else if(angle == 90 || angle == -270){
                face.y = (int)(face.y * scale - face.height);
                face.x = face.x - face.width/4;
                face.y = face.y - face.height/4;
            }
        }
        return faces;
    }

    public static Rect getMirroredFaceForFrontCamera(Mat img, Rect face){
        int topLeftX = (int) (img.cols() - (face.tl().x + face.width));
        int bottomRightX = (int) (img.cols() - (face.br().x) + face.width);
        Point tl = new Point(topLeftX, face.tl().y);
        Point br = new Point(bottomRightX, face.br().y);
        return new Rect(tl, br);
    }
}
