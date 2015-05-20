package com.jiaminglu.json.demo;

import com.jiaminglu.json.JsonObject;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jiaminglu on 15/2/17.
 */
public class Demo extends JsonObject {
    @JsonField public int i;
    @JsonField public String s;
    @JsonField(optional = true) public Integer ii;
    @JsonField(optional = true) public long l;
    @JsonField(optional = true) public Long ll;
    @JsonField(optional = true) public double d;
    @JsonField public Double dd;
    @JsonField public float f;
    @JsonField public Float ff;
    @JsonField public String[] ss;
    @JsonField(array_size = 2) int[][] haha;
    @JsonField(date_format = "yyyy", date_timezone = "GMT+8") Date year;
    @JsonField(date_format = "yyyy-MM-dd HH:mm:ss", date_timezone = "$date_offset") Date date_with_offset;
    @JsonField String date_offset;

    static class InnerDemo extends JsonObject {
        @JsonField public int i;
    }
    @JsonField InnerDemo inner;

    static class ExtendedInner extends InnerDemo {
        @JsonField public int iiii;
    }

    @JsonField public ExtendedInner[][] extendedInners;

    public static void main(String[] args) {
        Demo demo;
        try {
            demo = new Demo().init("{year:\"2012\",date_with_offset:\"2015-5-20 16:03:00\",date_offset:\"GMT+9\",i:3,s:\"aa\", ii:4,l:32232,dd:5.6,f:3,ff:23.2,inner:{i:6},ss:[\"hello\", \"world\"],extendedInners:[[{i:1,iiii:2},{i:11,iiii:22}]],haha:[[1,2],[2,3]]}");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        System.out.println(demo.i);
        System.out.println(demo.s);
        System.out.println(demo.ii);
        System.out.println(demo.l);
        System.out.println(demo.ll);
        System.out.println(demo.d);
        System.out.println(demo.dd);
        System.out.println(demo.f);
        System.out.println(demo.ff);
        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(demo.year));
        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(demo.date_with_offset));
        System.out.println(demo.date_offset);
        System.out.println(demo.inner.i);
        System.out.println(demo.toString());
    }
}
