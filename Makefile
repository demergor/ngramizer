SRC_DIR := src
OUT_DIR := out
BIN_DIR := bin

JAR_NAME := ngramizer.jar
MANIFEST := manifest.txt

SOURCES := $(shell find $(SRC_DIR) -name "*.java")


all: jar

compile: 
	javac -d $(OUT_DIR) $(SOURCES)

jar: compile
	jar -cfm $(BIN_DIR)/$(JAR_NAME) $(MANIFEST) -C $(OUT_DIR) .

clean: 
	rm -rf $(OUT_DIR) $(BIN_DIR)/$(JAR_NAME)
