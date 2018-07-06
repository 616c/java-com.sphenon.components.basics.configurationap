package com.sphenon.basics.configuration.annotations.processor;

/****************************************************************************
  Copyright 2001-2018 Sphenon GmbH

  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  License for the specific language governing permissions and limitations
  under the License.
*****************************************************************************/

import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.tools.*;
import java.util.*;
import java.io.*;

import static java.util.Collections.*;

import com.sphenon.basics.configuration.annotations.*;

@SupportedAnnotationTypes("com.sphenon.basics.configuration.annotations.Configuration")
// @SupportedOptions(..._6)
@SupportedSourceVersion(SourceVersion.RELEASE_7) 
public class ConfigurationAnnotationProcessor extends AbstractProcessor {

    public ConfigurationAnnotationProcessor() {
        super();
    }

    @Override
    public boolean process(Set<? extends TypeElement> all_annotations, RoundEnvironment environment) {
        Set<? extends Element> elements = environment.getElementsAnnotatedWith(Configuration.class);
        for (Element element : elements) {
            List<? extends AnnotationMirror> annotations = element.getAnnotationMirrors();
            if (element.getKind() == ElementKind.INTERFACE) {
                // processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Interface: " + element.getSimpleName());
                Configuration        ca      = element.getAnnotation(Configuration.class);
                Element              ecls    = element.getEnclosingElement();
                if (ecls != null && ecls.getKind() == ElementKind.CLASS) {
                    Element epkg = ecls;
                    while (epkg != null && epkg.getKind() != ElementKind.PACKAGE) {
                        epkg = epkg.getEnclosingElement();
                    }

                    String qname = ((QualifiedNameable) element).getQualifiedName().toString();
                    String pname = ((QualifiedNameable) epkg).getQualifiedName().toString();
                    String iname = ecls.getSimpleName().toString();
                    String cname = "Configuration_" + iname;

                    try {
                        JavaFileObject jfo = processingEnv.getFiler().createSourceFile(pname + "." + cname);
                        Writer jfw = jfo.openWriter();
                        PrintWriter pw = new PrintWriter(jfw);
                        pw.println("package " + pname + ";");
                        pw.println("");
                        pw.println("import com.sphenon.basics.context.*;");
                        pw.println("import com.sphenon.basics.customary.*;");
                        pw.println("import com.sphenon.basics.exception.*;");
                        pw.println("import com.sphenon.basics.configuration.*;");
                        pw.println("");
                        pw.println("public class " + cname + " implements " + qname + " {");
                        pw.println("");
                        pw.println("    protected Configuration configuration;");
                        pw.println("");
                        pw.println("    protected " + cname + " (CallContext context) {");
                        pw.println("        configuration = Configuration.create(context, \"" + pname + "." + iname + "\");");
                        pw.println("    }");
                        pw.println("");
                        pw.println("    static public " + cname + " get (CallContext context) {");
                        pw.println("        return new " + cname + "(context);");
                        pw.println("    }");
                        List<? extends Element> childs = element.getEnclosedElements();
                        for (Element child : childs) {
                            if (child.getKind() == ElementKind.METHOD) {
                                ExecutableElement method = (ExecutableElement) child;
                                Required     r  = method.getAnnotation(Required.class);
                                DefaultValue dv = method.getAnnotation(DefaultValue.class);
                                String       t  = method.getReturnType().toString();
                                List<? extends VariableElement> mps = method.getParameters();
                                String       n = method.getSimpleName().toString();
                                boolean      first;
                                if (n.matches("get(.*)")) {
                                    n = n.substring(3);

                                    pw.println("");
                                    pw.print("    public " + t + " get" + n + "(CallContext context");
                                    first = true;
                                    for (VariableElement mp : mps) {
                                        if (first && mp.asType().toString().matches(".*CallContext$")) {
                                            continue;
                                        }
                                        first = false;
                                        pw.print(", " + mp.asType() + " " + mp.getSimpleName());
                                    }
                                    pw.println(") {");
                                    pw.print("        String entry = \"" + n);
                                    first = true;
                                    for (VariableElement mp : mps) {
                                        if (first && mp.asType().toString().matches(".*CallContext$")) {
                                            continue;
                                        }
                                        first = false;
                                        pw.print(".\" + " + mp.getSimpleName() + " + \"");
                                    }
                                    pw.println("\";");
                                    if (r != null) {
                                        pw.print("        try {\n    ");
                                    }
                                    pw.println("        return configuration." + (r == null ? "get" : ("mustGet")) + "(context, entry, " + (dv == null ? ("(" + t + ") " + (getNullValue(t))) : (dv.value())) + ");");
                                    if (r != null) {
                                        pw.println("        } catch(ConfigurationEntryNotFound cenf) {");
                                        pw.println("            CustomaryContext.create((Context)context).throwConfigurationError(context, \"Required configuration entry '%(entry)' not found\", \"entry\", entry);");
                                        pw.println("            throw (ExceptionConfigurationError) null; // compiler insists");
                                        pw.println("        }");
                                    }
                                    pw.println("    }");
                                } else if (n.matches("set(.*)")) {
                                    n = n.substring(3);

                                    pw.println("");
                                    pw.print("    public void set" + n + "(CallContext context");
                                    first = true;
                                    for (VariableElement mp : mps) {
                                        if (first && mp.asType().toString().matches(".*CallContext$")) {
                                            continue;
                                        }
                                        first = false;
                                        pw.print(", " + mp.asType() + " " + mp.getSimpleName());
                                    }
                                    pw.println(") {");
                                    pw.print("        configuration.set(context, \"" + n);
                                    first = true;
                                    int i=0;
                                    String val_arg_name = null;
                                    for (VariableElement mp : mps) {
                                        if (first && mp.asType().toString().matches(".*CallContext$")) {
                                            i++;
                                            continue;
                                        }
                                        first = false;
                                        if (i < mps.size() - 1) {
                                            pw.print(".\" + " + mp.getSimpleName() + " + \"");
                                        } else {
                                            val_arg_name = mp.getSimpleName().toString();
                                        }
                                        i++;
                                    }
                                    pw.println("\", " + val_arg_name + ");");
                                    pw.println("    }");
                                }
                            }
                        }
                        pw.println("}");
                        pw.close();
                        jfw.close();
                    } catch (IOException ioe) {
                        System.err.println("dumm gelaufen " + ioe);
                    }
                }
            }


        }

        return true;
    }

    public String getNullValue(String typename) {
        if (typename == null) { return "???"; }
        if (typename.equals("boolean" )) { return "false" ; }
        if (typename.equals("byte"    )) { return "0"     ; }
        if (typename.equals("char"    )) { return "' '"   ; }
        if (typename.equals("short"   )) { return "0"     ; }
        if (typename.equals("int"     )) { return "0"     ; }
        if (typename.equals("long"    )) { return "0L"    ; }
        if (typename.equals("float"   )) { return "0.0F"  ; }
        if (typename.equals("double"  )) { return "0.0"   ; }
        return "null";
    }
}
