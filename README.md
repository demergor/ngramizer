***Text file analyzer*** that analyzes various ngram stats, such as: 
- letter frequencies
- bigrams
- trigrams
- skipgrams (letter pairs that are separated by one arbitrary character)
- spacegrams (letter pairs that are separated by one space)
- respective asymmetries

***

Parameters (CLI): 
- input path (file) *(implementation of directory inputs is in progress)* 
- output path (directory)
- regex (optional)

***

Regex: 
- must be specified with java-compatible patterns
- everything that matches the regex will be **removed** in the normalization process
- if no regex is given, normalization will be skipped entirely

***

Asymmetries:
- a metric to determine the distribution of ngrams:
    - "ae" != "ea" 
    - asym(ea, ae) = abs(freq(ea) - freq(ae))

*Results will be saved in form of .txt files in the specified output path*

***

Work in progress: 

***

Not yet implemented/Planned features: 
- Support for input directories, as opposed to only files (Reader.java)
- Asymmetry calculation (Analyzer.java)
