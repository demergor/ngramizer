***Text file analyzer*** that analyzes various ngram stats, such as: 
- letter frequencies
- bigrams
- trigrams
- skipgrams (letter pairs that are separated by one arbitrary character)
- spacegrams (letter pairs that are separated by one space)
- respective asymmetries

***

**Parameters (CLI):**
- input path (file) *(implementation of directory inputs is in progress)* 
- output path (directory)
- regex (optional)

*Example call from the terminal:*
**java jar ngramizer /path/of/output/directory /path/of/input/file "\[abc\]+"**

***

*Regex:*
- **must be specified with java-compatible patterns (java.util.regex.Pattern)**
- everything that matches the regex will be ***removed*** in the normalization process
- if no regex is given, normalization will be skipped entirely

- pre-specified regex patterns:
    - standard: *alphanumeric, but with standard punctuation symbols*
        - included symbols: . , ! ? ; : \ " ( ) \[ \] { } \- € @ # $ % & * | / ~ ^ ° < > + _ ` ´

    - alphabetic: *only letters in the German alphabet*
    - numeric: *only digits*

    - alphanumeric: *only digits and letters in the German alphabet*

    - unicode: *unicode letters and digits (without emojis or symbols)*

    - nonletters: *everything except letters from the German alphabet*

*Note: spaces are not removed in any of the pre-specified regex patterns* 

***

*Asymmetries:*
- a metric to determine the distribution of ngrams:
    - "ae" != "ea" 
    - asym(ea, ae) = abs(freq(ea) - freq(ae))

*Results will be saved in form of .txt files in the specified output path*

***

**Work in progress:**
- Consider doing the repeated-letter-elimination on result-basis, not on normalization-basis

***

**Not yet implemented/Planned features:** 
- Support for input directories, as opposed to only files (Reader.java)

- Analyzation and output of more general metadata (line count, words) 
    (Analyzer.java, Ngrams.java)
