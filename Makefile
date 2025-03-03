OUT_DIR := .github/bin

.PHONY: test
test: build
	@ java -cp $(OUT_DIR)/out test.test

.PHONY: build
build:
	@ export OCAMLRUNPARAM=b && \
		clj2js compile -target repl -src build.clj > .github/Makefile
	@ $(MAKE) -f .github/Makefile
	@ mkdir -p $(OUT_DIR)/y2k && \
		clj2js gen -target java > $(OUT_DIR)/y2k/RT.java
	@ cd $(OUT_DIR) && javac -d out **/*.java

.PHONY: clean
clean:
	@ rm -rf $(OUT_DIR)
