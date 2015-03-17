package com.jiaminglu.json;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by jiaminglu on 15/2/23.
 */
public class JsonObject {
    private JsonHelper helper;
    public JsonHelper getHelper() throws JSONException {
        if (helper == null)
            try {
                helper = (JsonHelper) Class.forName(getClass().getName() + "_helper").newInstance();
            } catch (Exception e) {
                throw new JSONException(e);
            }
        return helper;
    }
    public void setHelper(JsonHelper helper) {
        this.helper = helper;
    }
    @Retention(RetentionPolicy.SOURCE)
    public @interface JsonField {
        static String NULL = "";
        String value() default NULL;
        String name() default NULL;
        String date_format() default NULL;
        String date_timezone() default NULL;
        int array_size() default -1;
        boolean optional() default false;
    }
    public interface JsonHelper {
        public void init(JsonObject target, JSONObject object) throws JSONException;
        public JSONObject toJSON(JsonObject target) throws JSONException;
    }
    public <T extends JsonObject> T init(JSONObject object) throws JSONException {
        getHelper().init(this, object);
        return (T) this;
    }
    public <T extends JsonObject> T init(String json) throws JSONException {
        getHelper().init(this, new JSONObject(json));
        return (T) this;
    }
    public JSONObject toJSON() throws JSONException {
        return getHelper().toJSON(this);
    }
    public String toString() {
        try {
            return toJSON().toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
