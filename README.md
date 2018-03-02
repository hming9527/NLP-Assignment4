# NLP-Assignment4 Viterbi Algorithm
## Score
Calculate probabilities without taking logarithm: 65.47.

Calculate probabilities and take the logarithm of probabilities:

* Uniform probability for unknown words: 94.93.

* Different probabilities for unknown words: 95.76.

## Approach
My initial approach was very simple. Read the training data and build the transition probability table and the emission probability table. However it gave a score of 65.47, just a little better than random guessing.

It came to me that the probabilties are all very small, using the probabilities alone coule lead to some inaccurate results. So I chose to take the logarithm of probabilities and re-generate the result on the development corpus. This simple move gave me a score of 94.93.

I was assigning a uniform probability to all unknown words till the second step. After that I decided to further improve the result by handling unknown words based on the morphology of English. My original plan was to use both the prefix and suffix of a word to better help determine the tag of an unknown word. But after some search, I decided to use suffix only, as the part of speech of a word can often be determined by the suffix reliably but not the prefix. 

First I'll still assign a uniform probability to an unknown word, then before actually writing the output to the file, I'll examine if the word ends with any certain suffix in my suffix vocabulary, if it does that the tag of that word will be changed to the corresponding tag. This small improvement gave me a score of 95.76.
