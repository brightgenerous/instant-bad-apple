package com.brightgenerous.instantbadapple;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class YoutubeUtils {

    public static String extractUrl(String url) throws IOException {
        if (url == null) {
            return null;
        }

        String ret = null;

        List<Video> videos = getStreamingUrisFromUrl(url);
        if ((videos != null) && !videos.isEmpty()) {
            String retUrl = null;
            for (Video video : videos) {
                if (video.ext.toLowerCase().contains("mp4")
                        && video.type.toLowerCase().contains("medium")) {
                    retUrl = video.url;
                    break;
                }
            }
            if (retUrl == null) {
                for (Video video : videos) {
                    if (video.ext.toLowerCase().contains("mp4")
                            && video.type.toLowerCase().contains("low")) {
                        retUrl = video.url;
                        break;
                    }
                }
            }
            if (retUrl == null) {
                for (Video video : videos) {
                    if (video.ext.toLowerCase().contains("mp4")) {
                        retUrl = video.url;
                        break;
                    }
                }
            }
            if (retUrl == null) {
                for (Video video : videos) {
                    if (video.ext.toLowerCase().contains("flv")
                            && video.type.toLowerCase().contains("medium")) {
                        retUrl = video.url;
                        break;
                    }
                }
            }
            if (retUrl == null) {
                for (Video video : videos) {
                    if (video.ext.toLowerCase().contains("flv")
                            && video.type.toLowerCase().contains("low")) {
                        retUrl = video.url;
                        break;
                    }
                }
            }
            if (retUrl == null) {
                for (Video video : videos) {
                    if (video.ext.toLowerCase().contains("flv")) {
                        retUrl = video.url;
                        break;
                    }
                }
            }
            ret = retUrl;
        }

        return ret;
    }

    private static final Map<String, Meta> typeMap = new HashMap<>();
    static {
        typeMap.put("13", new Meta("13", "3GP", "Low Quality - 176x144"));
        typeMap.put("17", new Meta("17", "3GP", "Medium Quality - 176x144"));
        typeMap.put("36", new Meta("36", "3GP", "High Quality - 320x240"));
        typeMap.put("5", new Meta("5", "FLV", "Low Quality - 400x226"));
        typeMap.put("6", new Meta("6", "FLV", "Medium Quality - 640x360"));
        typeMap.put("34", new Meta("34", "FLV", "Medium Quality - 640x360"));
        typeMap.put("35", new Meta("35", "FLV", "High Quality - 854x480"));
        typeMap.put("43", new Meta("43", "WEBM", "Low Quality - 640x360"));
        typeMap.put("44", new Meta("44", "WEBM", "Medium Quality - 854x480"));
        typeMap.put("45", new Meta("45", "WEBM", "High Quality - 1280x720"));
        typeMap.put("18", new Meta("18", "MP4", "Medium Quality - 480x360"));
        typeMap.put("22", new Meta("22", "MP4", "High Quality - 1280x720"));
        typeMap.put("37", new Meta("37", "MP4", "High Quality - 1920x1080"));
        typeMap.put("33", new Meta("38", "MP4", "High Quality - 4096x230"));
    }

    private static final Pattern patternStreamMap = Pattern.compile("stream_map\": \"(.*?)?\"");

    private static final Pattern patternItag = Pattern
            .compile("^(?:.*\\?|.*&)?itag=([0-9]+?)(?:&.*)?$");

    private static final Pattern patternSig = Pattern.compile("^(?:.*\\?|.*&)?sig=(.*?)(?:&.*)?$");

    private static final Pattern patternUrl = Pattern.compile("^(?:.*\\?|.*&)?url=(.*?)(?:&.*)?$");

    private static List<Video> getStreamingUrisFromUrl(String url) throws IOException {
        String html = getPageHtml(url);
        if (html == null) {
            return null;
        }

        return parseAsVideoHtml(html);
    }

    private static List<Video> parseAsVideoHtml(String html) throws IOException {
        if (html == null) {
            return null;
        }

        html = html.replace("\\u0026", "&");

        // Parse the HTML response and extract the streaming URIs
        if (html.contains("verify-age-thumb")) {
            return null;
        }

        if (html.contains("das_captcha")) {
            return null;
        }

        List<String> streamMaps = new ArrayList<>();
        {
            Matcher mStreamMap = patternStreamMap.matcher(html);
            while (mStreamMap.find()) {
                streamMaps.add(mStreamMap.group(1));
            }
        }

        if (streamMaps.size() != 1) {
            return null;
        }

        Map<String, String> foundItags = new HashMap<>();
        String urls[] = streamMaps.get(0).split(",");
        if ((urls != null) && (0 < urls.length)) {
            for (String ppUrl : urls) {
                String _url = URLDecoder.decode(ppUrl, "UTF-8");

                Matcher mItag = patternItag.matcher(_url);
                String itag = null;
                if (mItag.find()) {
                    itag = mItag.group(1);
                }

                Matcher mSig = patternSig.matcher(_url);
                String sig = null;
                if (mSig.find()) {
                    sig = mSig.group(1);
                }

                Matcher mUrl = patternUrl.matcher(ppUrl);
                String _u = null;
                if (mUrl.find()) {
                    _u = mUrl.group(1);
                }

                if ((itag != null) && (sig != null) && (_u != null)) {
                    foundItags.put(itag, URLDecoder.decode(_u, "UTF-8") + "&" + "signature=" + sig);
                }
            }
        }

        if (foundItags.size() == 0) {
            return null;
        }

        List<Video> videos = new ArrayList<>();

        for (Entry<String, Meta> entry : typeMap.entrySet()) {
            String format = entry.getKey();
            Meta meta = entry.getValue();
            if (foundItags.containsKey(format)) {
                videos.add(new Video(meta.ext, meta.type, foundItags.get(format)));
            }
        }

        return videos;
    }

    private static String getPageHtml(String url) throws IOException {
        if (url == null) {
            return null;
        }

        // [x] keep only first argument
        // [ ] keep only argument "v"

        // Remove any query params in query string after the watch?v=<vid> in
        // e.g.
        // http://www.youtube.com/watch?v=0RUPACpf8Vs&feature=youtube_gdata_player
        {
            int andIdx = url.indexOf('&');
            if (0 <= andIdx) {
                url = url.substring(0, andIdx);
            }
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new URL(url).openStream(), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }

        return sb.toString();
    }
}

class Meta {

    public String num;

    public String type;

    public String ext;

    Meta(String num, String ext, String type) {
        this.num = num;
        this.ext = ext;
        this.type = type;
    }
}

class Video {

    public String ext;

    public String type;

    public String url;

    Video(String ext, String type, String url) {
        this.ext = ext;
        this.type = type;
        this.url = url;
    }
}
