package com.tomerrosenfeld.tweaksforgo;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SecretConstants {
    public static String getPropertyValue(Context context, String propertyName) {
        InputStream input;
        String propertyValue;
        try {
            input = context.getAssets().open("secretconstants.properties");
            Properties properties = new Properties();
            properties.load(input);
            propertyValue = properties.getProperty(propertyName);
        } catch (IOException e) {
            // file not found
            propertyValue = "";
        }
        return propertyValue;
    }
}

