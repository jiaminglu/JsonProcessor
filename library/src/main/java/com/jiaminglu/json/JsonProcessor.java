package com.jiaminglu.json;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by jiaminglu on 15/2/17.
 */
@SupportedAnnotationTypes({
        "com.jiaminglu.json.JsonObject.JsonField"
})
public class JsonProcessor extends AbstractProcessor {

    HashMap<String, ArrayList<Element>> elements = new HashMap<String, ArrayList<Element>>();
    HashMap<String, String> superClasses = new HashMap<String, String>();
    HashMap<String, String> realNames = new HashMap<String, String>();
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Messager messager = processingEnv.getMessager();
        for (TypeElement  te : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(te)) {
                TypeElement typeElement = (TypeElement) element.getEnclosingElement();
                String classQName = typeElement.getQualifiedName().toString();
                Element e = typeElement;
                while (e.getKind() != ElementKind.PACKAGE) {
                    e = e.getEnclosingElement();
                }
                int packageLength = e.toString().length();
                String realName = classQName.substring(0, packageLength + 2) + classQName.substring(packageLength + 2).replace(".", "$");
                realNames.put(classQName, realName);
                superClasses.put(classQName, typeElement.getSuperclass().toString());

                if (!elements.containsKey(classQName)) {
                    elements.put(classQName, new ArrayList<Element>());
                }
                elements.get(classQName).add(element);
            }
        }

        for (Map.Entry<String, ArrayList<Element>> entry : elements.entrySet()) {
            PrintWriter w = null;

            String classQName = entry.getKey();
            String realName = realNames.get(classQName);
            String superClass = superClasses.getOrDefault(classQName, "java.lang.Object");

            boolean hasSuperClass = false;
            if (superClasses.containsKey(superClass)) {
                superClass = realNames.get(superClass) + "_helper";
                hasSuperClass = true;
            } else {
                superClass = "com.jiaminglu.json.JsonObject.JsonHelper";
            }

            try {
                w = new PrintWriter(
                        processingEnv.getFiler()
                                .createSourceFile(realName + "_helper", entry.getValue().toArray(new Element[entry.getValue().size()]))
                                .openWriter());
                w.println(String.format(        "package %s;", realName.substring(0, realName.lastIndexOf('.'))));
                w.println(String.format(        "public class %s_helper extends %s {", realName.substring(realName.lastIndexOf('.') + 1), superClass));

                for (Element element : entry.getValue()) {
                    String name = element.getSimpleName().toString();
                    JsonObject.JsonField jsonField = element.getAnnotation(JsonObject.JsonField.class);
                    if (!jsonField.date_format().equals(JsonObject.JsonField.NULL)) {
                        w.println(String.format("    static java.text.SimpleDateFormat %s_$format = new java.text.SimpleDateFormat(\"%s\");", name, jsonField.date_format()));
                        if (!jsonField.date_timezone().equals(JsonObject.JsonField.NULL)) {
                            w.println(String.format("    static { %s_$format.setTimeZone(java.util.TimeZone.getTimeZone(\"%s\")); }", name, jsonField.date_timezone()));
                        }
                    }
                }
                w.println(                      "    public void init (com.jiaminglu.json.JsonObject targetObject, org.json.JSONObject object) throws org.json.JSONException {");
                w.println(String.format(        "        %s target = (%s) targetObject;", classQName, classQName));
                if (hasSuperClass)
                    w.println(                  "        super.init(target, object);");
                for (Element element : entry.getValue()) {
                    String type = element.asType().toString();
                    String name = element.getSimpleName().toString();
                    JsonObject.JsonField jsonField = element.getAnnotation(JsonObject.JsonField.class);
                    String jsonFieldName = jsonField.name().equals(JsonObject.JsonField.NULL) ? name : jsonField.name();
                    w.println(                  "        {");
                    if (jsonField.optional()) {
                        w.println(String.format("            Object in = object.opt(\"%s\");", jsonFieldName));
                        w.println(              "            if (in != null && in != org.json.JSONObject.NULL) {");
                        String out = readObj(w, name, jsonField, type, "in", 0);
                        w.println(String.format("                target.%s = %s;", name, out));
                        w.println(              "            }");
                    } else {
                        String out = readObj(w, name, jsonField, type, String.format("object.opt(\"%s\")", jsonFieldName), 0);
                        w.println(String.format("            target.%s = %s;", name, out));
                    }
                    w.println(                  "        }");
                }
                w.println(                      "    }");
                w.println(                      "    public org.json.JSONObject toJSON(com.jiaminglu.json.JsonObject targetObject) throws org.json.JSONException {");
                w.println(String.format(        "        %s target = (%s) targetObject;", classQName, classQName));
                if (hasSuperClass)
                    w.println(                  "        org.json.JSONObject object = super.toJSON(target);");
                else
                    w.println(                  "        org.json.JSONObject object = new org.json.JSONObject();");
                for (Element element : entry.getValue()) {
                    String type = element.asType().toString();
                    String name = element.getSimpleName().toString();
                    JsonObject.JsonField jsonField = element.getAnnotation(JsonObject.JsonField.class);
                    String jsonFieldName = jsonField.name().equals(JsonObject.JsonField.NULL) ? name : jsonField.name();
                    w.println(                  "        {");
                    String out = writeObj(w, name, jsonField, type, "target." + jsonFieldName, 0);
                    w.println(String.format(    "            object.put(\"%s\", %s);", jsonFieldName, out));
                    w.println(                  "        }");
                }
                w.println(                      "        return object;");
                w.println(                      "    }");
                w.println(                      "}");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (w != null)
                    w.close();
            }
        }

        return true;
    }

    String writeObj(PrintWriter w, String name, JsonObject.JsonField jsonField, String type, String in, int i) {
        if (type.equals("java.lang.String")
                ||type.equals("byte") || type.equals("java.lang.Byte")
                || type.equals("short") || type.equals("java.lang.Short")
                || type.equals("int") || type.equals("java.lang.Integer")
                || type.equals("long") || type.equals("java.lang.Long")
                || type.equals("float") || type.equals("java.lang.Float")
                || type.equals("double") || type.equals("java.lang.Double")) {
            return in;
        } else if (type.equals("java.util.Date")) {
            return String.format("%s_$format.format(%s)", name, in);
        } else if (type.endsWith("[]")) {
            w.println(    String.format("            org.json.JSONArray outArray%d = new org.json.JSONArray();", i));
            w.println(    String.format("            for (%s i%d : %s) {", type.substring(0, type.length() - 2), i, in));
            if (type.contains(".")) {
                w.println(String.format("                if (i%d != %s) {", i, "null"));
                String out = writeObj(w, null, null, type.substring(0, type.length() - 2), String.format("i%d", i), i + 1);
                w.println(String.format("                    outArray%d.put(%s);", i, out));
                w.println(String.format("                } else {"));
                w.println(String.format("                    outArray%d.put(org.json.JSONObject.NULL);", i));
                w.println(String.format("                }"));
            } else {
                String out = writeObj(w, null, null, type.substring(0, type.length() - 2), String.format("i%d", i), i + 1);
                w.println(String.format("                    outArray%d.put(%s);", i, out));
            }
            w.println(    String.format("            }"));
            return String.format("outArray%d", i);
        } else {
            return String.format("%s.toJSON()", in);
        }
    }

    String readObj(PrintWriter w, String name, JsonObject.JsonField jsonField, String type, String in, int i) {
        if (type.equals("java.lang.String")) {
            return String.format("((java.lang.String) %s)", in);
        } else if (type.equals("boolean") || type.equals("java.lang.Boolean")) {
            return String.format("((java.lang.Boolean) %s)", in);
        } else if (type.equals("byte") || type.equals("java.lang.Byte")) {
            return readPrim("byte", "java.lang.Byte", in);
        } else if (type.equals("short") || type.equals("java.lang.Short")) {
            return readPrim("short", "java.lang.Short", in);
        } else if (type.equals("int") || type.equals("java.lang.Integer")) {
            return readPrim("int", "java.lang.Integer", in);
        } else if (type.equals("long") || type.equals("java.lang.Long")) {
            return readPrim("long", "java.lang.Long", in);
        } else if (type.equals("float") || type.equals("java.lang.Float")) {
            return readPrim("float", "java.lang.Float", in);
        } else if (type.equals("double") || type.equals("java.lang.Double")) {
            return readPrim("double", "java.lang.Double", in);
        } else if (type.equals("java.util.Date")) {
            w.println(String.format("            java.util.Date date;"));
            w.println(String.format("            try {"));
            w.println(String.format("                date = %s_$format.parse((java.lang.String)%s);", name, in));
            w.println(String.format("            } catch (java.text.ParseException e) {"));
            w.println(String.format("                throw new org.json.JSONException(e);"));
            w.println(String.format("            }"));
            return "date";
        } else if (type.endsWith("[]")) {
            String elemType = type.substring(0, type.length() - 2);
            String arrayMarker = "";
            while (elemType.endsWith("[]")) {
                elemType = elemType.substring(0, elemType.length() - 2);
                arrayMarker += "[]";
            }
            w.println(String.format("            org.json.JSONArray array%d = (org.json.JSONArray) %s;", i, in));
            w.println(String.format("            %s outArray%d = new %s[array%d.length()]%s;", type, i, elemType, i, arrayMarker));
            w.println(String.format("            for (int i%d = 0; i%d < outArray%d.length; i%d++) {", i, i, i, i));
            String out = readObj(w, null, null, elemType + arrayMarker, String.format("array%d.get(i%d)", i, i), i + 1);
            w.println(String.format("                outArray%d[i%d] = %s;", i, i, out));
            w.println(String.format("            }"));
            if (jsonField != null && jsonField.array_size() != -1)
                w.println(String.format("            if (outArray%d.length != %d) throw new org.json.JSONException(\"array length mismatch\");", i, jsonField.array_size()));
            return String.format("outArray%d", i);
        } else {
            w.println(String.format("            %s out = new %s();", type, type));
            w.println(String.format("            out.setHelper(new %s_helper());", realNames.get(type)));
            w.println(String.format("            out.init((org.json.JSONObject) %s);", in));
            return "out";
        }
    }

    String readPrim(String primType, String objType, String in) {
        return String.format(
                "(%s instanceof java.lang.Number ? ((java.lang.Number) %s).%sValue() : %s.parse%s((java.lang.String) %s))",
                in, in, primType, objType, primType.substring(0,1).toUpperCase() + primType.substring(1), in);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
