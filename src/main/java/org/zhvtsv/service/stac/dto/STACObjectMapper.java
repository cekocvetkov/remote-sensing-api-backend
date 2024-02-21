package org.zhvtsv.service.stac.dto;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class STACObjectMapper {

    public static List<STACItemPreview> getStacItemsPreview(List<String> jsonBodyStrings) {
        List<STACItemPreview> stacItemPreviewList = new ArrayList<>();

        for(String jsonBodyString: jsonBodyStrings) {
            JSONObject json = new JSONObject(jsonBodyString);
            JSONArray featuresJsonArray = json.getJSONArray("features");
            for (int i = 0; i < featuresJsonArray.length(); i++) {
                JSONObject featureJSON = featuresJsonArray.getJSONObject(i);
                STACItemPreview stacItemPreview = getStacItemPreview(featureJSON);
                stacItemPreviewList.add(stacItemPreview);
            }
        }
        return stacItemPreviewList;
    }

    public static STACItemPreview getStacItemPreview(JSONObject jsonBody) {
        STACItemPreview stacItemPreview = new STACItemPreview();
        String id = jsonBody.getString("id");
        String downloadUrl = "";
        System.out.println(jsonBody.getJSONObject("assets"));
        if(jsonBody.getJSONObject("assets").optJSONObject("image") != null) { //Planet Computer
            downloadUrl = jsonBody.getJSONObject("assets").getJSONObject("image").getString("href");
        } else {
            downloadUrl = jsonBody.getJSONObject("assets").getJSONObject("visual").getString("href");
        }
        String thumbnailUrl = jsonBody.getJSONObject("assets").getJSONObject("thumbnail").getString("href");
        stacItemPreview.setId(id);
        stacItemPreview.setThumbnailUrl(thumbnailUrl);
        stacItemPreview.setDownloadUrl(downloadUrl);

        return stacItemPreview;
    }
}
