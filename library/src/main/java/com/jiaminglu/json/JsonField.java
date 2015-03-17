package com.jiaminglu.json;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by jiaminglu on 15/3/11.
 */
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