package org.zhvtsv.utils;


import java.net.URISyntaxException;
import java.nio.file.Paths;

public class PathUtils
{
    public static String getPathForImageInResources(String imageName){
        try
        {
            return Paths.get( PathUtils.class.getClassLoader().getResource(imageName).toURI()).toString();
        }
        catch ( URISyntaxException e )
        {
            throw new RuntimeException( e );
        }
    }
}