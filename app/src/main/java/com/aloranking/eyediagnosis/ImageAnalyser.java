package com.aloranking.eyediagnosis;

import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.features2d.DescriptorExtractor;

public class ImageAnalyser {
    private static int descriptor = DescriptorExtractor.BRISK;
    private static String descriptorType;
    private static int min_dist = 10;
    private static int min_matches = 750;

   /* */
}
