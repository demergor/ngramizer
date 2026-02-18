***Text analyzer*** that analyzes various ngram stats, such as: 
- letter frequencies
- bigrams
- trigrams
- skipgrams (letter pairs that are separated by one arbitrary character)
- spacegrams (letter pairs that are separated by one space)
- respective asymmetries for the multi-gram types
- combinatorial frequencies (frequency anagram scores added together, e.g.: 
    - ae : 333
    - ea : 222

    - combinatorial frequency counterpart: 
        - ae : 555 (= ae + ea)
        - ea : 555 (= ea + ae)

***

**Parameters (CLI):**
- input path (file or directory) **(required)**
- output path (directory) **(effectively required)**
- regex (optional)
- flag for multi-letter-instance removal, e.g. "he**ll**o" -> "he**l**o" (true/false, default false)

*Example call from the terminal:*
**java jar ngramizer /path/of/output/directory /path/of/input/file "\[abc\]+" true**

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
**i.e. your/given/output/directory will be populated with the directories:**
- analyzed
    - analyzed/frequencies *(letter.txt, bigram.txt, trigram.txt, ...)*
        - analyzed/frequencies/combinatorial *(bigram.txt, trigram.txt, skipgram.txt, ...)*
    - analyzed/asymmetries *(bigram.txt, trigram.txt, skipgram.txt, ...)*
- normalized *(normalized.txt)*

***

**Work in progress:**
***

**Not yet implemented/Planned features:** 
- Make combinatorial frequency calculation multithreaded as its combinatorial nature is expensive in computation
