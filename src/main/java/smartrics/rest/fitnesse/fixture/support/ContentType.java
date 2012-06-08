/*  Copyright 2008 Andrew Ochsner
 *
 *  This file is part of RestFixture.
 *
 *  RestFixture (http://code.google.com/p/rest-fixture/) is free software:
 *  you can redistribute it and/or modify it under the terms of the
 *  GNU Lesser General Public License as published by the Free Software Foundation,
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  RestFixture is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with RestFixture.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  If you want to contact the author please leave a comment here
 *  http://smartrics.blogspot.com/2008/08/get-fitnesse-with-some-rest.html
 */
package smartrics.rest.fitnesse.fixture.support;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mortbay.log.Log;

import smartrics.rest.client.RestData.Header;
import smartrics.rest.config.Config;

/**
 * Supported content types.
 * 
 * @author fabrizio
 */
public enum ContentType {

    XML, JSON, TEXT, JS;    

    private static Map<String, ContentType> contentTypeToEnum = new HashMap<String, ContentType>();
    private static String defaultCharset;
        
    private static String vndExtPattern = "application/.+\\+(\\w+)";
    static {
        resetDefaultMapping();
    }

    public List<String> toMime() {
        List<String> types = new ArrayList<String>();
        for (Map.Entry<String, ContentType> e : contentTypeToEnum.entrySet()) {
            if (e.getValue().equals(this)) {
                types.add(e.getKey());
            }
        }
        return types;
    }
    
    private static ContentType matchVndType(String contentType) {
          // Compile and use regular expression for "+type"
            Pattern pattern = Pattern.compile(vndExtPattern);
            Matcher matcher = pattern.matcher(contentType.toString());
            boolean matchFound = matcher.find();
            
            ContentType result = null;
            if (matchFound) {
                String subType = matcher.group(1);
                result = ContentType.valueOf(subType.toUpperCase());                
            } 
            
            return result;
    }

    public static ContentType typeFor(String t) {
        ContentType r = contentTypeToEnum.get(t);
        if (r == null) {
                r = matchVndType(t);
                if( r == null) {
                    r = contentTypeToEnum.get("default");
                }
            
        }
        return r;
    }

    public static void config(Config config) {
        // TODO: set default charset
        defaultCharset = config.get("restfixture.content.default.charset", Charset.defaultCharset().name());
        String htmlConfig = config.get("restfixture.content.handlers.map", "");
        String configStr = Tools.fromHtml(htmlConfig);
        Map<String, String> map = Tools.convertStringToMap(configStr, "=", "\n");
        for (Map.Entry<String, String> e : map.entrySet()) {
            String value = e.getValue();
            String enumName = value.toUpperCase();            
            ContentType ct = ContentType.valueOf(enumName);
            if (null == ct) {
                ContentType[] values = ContentType.values();
                StringBuffer sb = new StringBuffer();
                sb.append("[");
                for (ContentType cType : values) {
                    sb.append("'").append(cType.toString()).append("' ");
                }
                sb.append("]");
                throw new IllegalArgumentException("I don't know how to handle " + value + ". Use one of " + values);
            }
            contentTypeToEnum.put(e.getKey(), ct);
        }
    }

    public static String parseCharset(List<Header> contentTypeHeaders) {
        if (contentTypeHeaders.size() != 1 || !"Content-Type".equalsIgnoreCase(contentTypeHeaders.get(0).getName())) {
            return defaultCharset;
        }
        String val = contentTypeHeaders.get(0).getValue();
        String[] vals = val.split(";");
        if (vals.length == 2) {
            String s = vals[1].trim();
            if (s.length() > 0) {
                try {
                    int pos = s.indexOf("charset=");
                    if (pos >= 0) {
                        s = s.substring(pos + "charset=".length());
                        return Charset.forName(s).name();
                    }
                } catch (RuntimeException e) {
                    Log.warn("Charset unknown or not possible to parse: " + s);
                }
            }
        }
        return defaultCharset;
    }

    public static void resetDefaultMapping() {
        contentTypeToEnum.clear();
        contentTypeToEnum.put("default", ContentType.XML);
        contentTypeToEnum.put("application/xml", ContentType.XML);
        contentTypeToEnum.put("application/json", ContentType.JSON);
        contentTypeToEnum.put("text/plain", ContentType.TEXT);
        contentTypeToEnum.put("application/x-javascript", ContentType.JS);
    }

    public static ContentType parse(List<Header> contentTypeHeaders) {        
        if (contentTypeHeaders.size() != 1 || !"Content-Type".equalsIgnoreCase(contentTypeHeaders.get(0).getName())) {
            return contentTypeToEnum.get("default");
        }
        String typeString = contentTypeHeaders.get(0).getValue();
        typeString = typeString.split(";")[0].trim();
        // TODO: capture encoding
        ContentType ret = contentTypeToEnum.get(typeString);       
        if (ret == null) {
            ret = matchVndType(typeString);
            if( ret == null) {
                 return contentTypeToEnum.get("default");
            }
           
        }
        return ret;
    }
}
