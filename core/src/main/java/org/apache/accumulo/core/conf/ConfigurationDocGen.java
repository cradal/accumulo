/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.core.conf;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.TreeMap;

/**
 * This class generates documentation to inform users of the available configuration properties in a
 * presentable form.
 */
class ConfigurationDocGen {
  private PrintStream doc;
  private final TreeMap<String,Property> sortedProps = new TreeMap<>();

  void generate() {
    pageHeader();

    beginTable("Property");
    for (Property prop : sortedProps.values()) {
      if (prop.getType() == PropertyType.PREFIX) {
        prefixSection(prop);
      } else {
        property(prop);
      }
    }

    beginSection("Property Types");
    beginTable("Type");
    propertyTypeDescriptions();

    doc.close();
  }

  void beginSection(String section) {
    doc.println("\n### " + section + "\n");
  }

  void beginTable(String name) {
    doc.println("| " + name + " | Description |");
    doc.println("|--------------|-------------|");
  }

  void pageHeader() {
    doc.println("---");
    doc.println("title: Server Properties");
    doc.println("category: administration");
    doc.println("order: 3");
    doc.println("---\n");
    doc.println("<!-- WARNING: Do not edit this file. It is a generated file"
        + " that is copied from Accumulo build (from core/target/generated-docs)" + " -->\n");
    doc.println("Below are properties set in `accumulo.properties` or the"
        + " Accumulo shell that configure Accumulo servers (i.e tablet server,"
        + " master, etc). Properties labeled 'Experimental' could be part of an incomplete"
        + " feature or have a higher risk of changing in the future.\n");
  }

  void prefixSection(Property prefix) {
    boolean depr = prefix.isDeprecated();
    doc.print("| <a name=\"" + prefix.getKey().replace(".", "_") + "prefix\" class=\"prop\"></a> **"
        + prefix.getKey() + "*** | ");
    doc.print(prefix.isExperimental() ? "**Experimental.** " : "");
    doc.println(
        (depr ? "**Deprecated.** " : "") + strike(sanitize(prefix.getDescription()), depr) + " |");
  }

  void property(Property prop) {
    boolean depr = prop.isDeprecated();
    doc.print("| <a name=\"" + prop.getKey().replace(".", "_") + "\" class=\"prop\"></a> "
        + prop.getKey() + " | ");
    doc.print(prop.isExperimental() ? "**Experimental.** " : "");
    doc.print(
        (depr ? "**Deprecated.** " : "") + strike(sanitize(prop.getDescription()), depr) + "<br>");
    doc.print(strike("**type:** " + prop.getType().name(), depr) + ", ");
    doc.print(strike("**zk mutable:** " + isZooKeeperMutable(prop), depr) + ", ");
    String defaultValue = sanitize(prop.getRawDefaultValue()).trim();
    if (defaultValue.length() == 0) {
      defaultValue = strike("**default value:** empty", depr);
    } else if (defaultValue.contains("\n")) {
      // deal with multi-line values, skip strikethrough of value
      defaultValue = strike("**default value:** ", depr) + "\n```\n" + defaultValue + "\n```\n";
    } else {
      if (prop.getType() == PropertyType.CLASSNAME
          && defaultValue.startsWith("org.apache.accumulo")) {
        defaultValue = strike("**default value:** " + "{% jlink -f " + defaultValue + " %}", depr);
      } else {
        defaultValue = strike("**default value:** " + "`" + defaultValue + "`", depr);
      }
    }
    doc.println(defaultValue + " |");
  }

  private String strike(String s, boolean isDeprecated) {
    return (isDeprecated ? "~~" : "") + s + (isDeprecated ? "~~" : "");
  }

  void propertyTypeDescriptions() {
    for (PropertyType type : PropertyType.values()) {
      if (type == PropertyType.PREFIX)
        continue;
      doc.println(
          "| " + sanitize(type.toString()) + " | " + sanitize(type.getFormatDescription()) + " |");
    }
  }

  String sanitize(String str) {
    return str.replace("\n", "<br>");
  }

  private ConfigurationDocGen(PrintStream doc) {
    this.doc = doc;
    for (Property prop : Property.values()) {
      this.sortedProps.put(prop.getKey(), prop);
    }
  }

  private String isZooKeeperMutable(Property prop) {
    if (!Property.isValidZooPropertyKey(prop.getKey()))
      return "no";
    if (Property.isFixedZooPropertyKey(prop))
      return "yes but requires restart of the " + prop.getKey().split("[.]")[0];
    return "yes";
  }

  /**
   * Generates documentation for accumulo.properties file usage. Arguments are: "--generate-markdown
   * filename"
   *
   * @param args
   *          command-line arguments
   * @throws IllegalArgumentException
   *           if args is invalid
   */
  public static void main(String[] args)
      throws FileNotFoundException, UnsupportedEncodingException {
    if (args.length == 2 && args[0].equals("--generate-markdown")) {
      new ConfigurationDocGen(new PrintStream(args[1], UTF_8.name())).generate();
    } else {
      throw new IllegalArgumentException(
          "Usage: " + ConfigurationDocGen.class.getName() + " --generate-markdown <filename>");
    }
  }
}
