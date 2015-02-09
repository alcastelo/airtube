OPT_JDK ?= /opt/jdk1.6.0_45/bin/
ANDROID_JAR ?= /opt/adt-bundle-linux-x86_64-20140321/sdk/platforms/android-18/android.jar

ifneq ($(wildcard $(OPT_JDK)), )
JDK = $(OPT_JDK)
endif
JAVAC = $(JDK)javac
JAR = $(JDK)jar
# note use newer javadoc because it looks much better
JAVADOC = javadoc -noqualifier all


VERSION := 1.0
OUT_DIR := package/airtube-$(VERSION)
TMP_DIR := package/tmp/

JAVA_SOURCES := $(shell find AirTubeAPI/src/ -name "*.java")
JAVA_SOURCES += $(shell find JavaLibrary/src/ -name "*.java")
JAVA_SOURCES += $(shell find AirTube/src/ -name "*.java")

API_SOURCES := $(shell find AirTubeAPI/src/ -name "*.java")
API_SOURCES += $(shell find AndroidAPI/src/ -name "*.java")
API_SOURCES += $(shell find AndroidAPI/gen/ -name "*.java")

LIB_SOURCES := $(shell find AndroidComponents/src/ -name "*.java")
LIB_SOURCES += $(shell find AndroidLibrary/src/ -name "*.java")
LIB_SOURCES += $(shell find JavaLibrary/src/ -name "*.java")

.PHONY: all outdir java android-api android-lib android javadoc zip clean

default: java

all: java android doc zip

outdir:
	-mkdir -p $(OUT_DIR)

java: outdir
	-mkdir -p $(TMP_DIR)/java
	$(JAVAC) -d $(TMP_DIR)/java $(JAVA_SOURCES)
	jar cvfe $(OUT_DIR)/airtube-java.jar com.thinktube.airtube.java.Main -C $(TMP_DIR)/java .

android-api: outdir
	-mkdir -p $(TMP_DIR)/android-api
	$(JAVAC) -cp $(ANDROID_JAR) -d $(TMP_DIR)/android-api $(API_SOURCES)
	jar cvf $(OUT_DIR)/airtube-android-api.jar -C $(TMP_DIR)/android-api .

android-lib: outdir
	-mkdir -p $(TMP_DIR)/android-lib
	$(JAVAC) -cp $(ANDROID_JAR):$(OUT_DIR)/airtube-android-api.jar -d $(TMP_DIR)/android-lib $(LIB_SOURCES)
	jar cvf $(OUT_DIR)/airtube-android-lib.jar -C $(TMP_DIR)/android-lib .

android: android-api android-lib

javadoc:
	$(JAVADOC) -d $(OUT_DIR)/doc/javadoc com.thinktube.airtube -sourcepath AirTubeAPI/src

doc: javadoc outdir
	mkdir -p $(OUT_DIR)/doc/
	cp LICENSE.txt $(OUT_DIR)/doc/
	cp README.md $(OUT_DIR)/doc/

zip:
	( cd package; zip -r airtube-$(VERSION).zip airtube-$(VERSION) )

clean:
	-rm -r package
