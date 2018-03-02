import java.io.*;
import java.util.*;

/**
 * Created by hming on 2/27/18.
 */
public class Viterbi {
    private static Map<String, Map<String, Double>> transitionCount = new HashMap<>();
    private static Map<String, Map<String, Double>> emissionCount = new HashMap<>();
    private static Map<String, Double> counter = new HashMap<>();
    private static List<String> tags = new ArrayList<>();
    private static final String START = "start";
    private static final String END = "end";
    private static final String[] nn_suffix = "acy|ance|ence|dom|er|or|ist|ian|eer|ism|ist|ity|ty|ment|nexx|ion|sion|tion|logy|age|hood|ary|ship".split("\\|");
    private static final String[] nns_suffix = "s|es|ies".split("\\|");
    private static final String[] verb_suffix ="ate|en|ify|fy|ize|ise".split("\\|");
    private static final String[] vbd_suffix = "ed".split("\\|");
    private static final String[] vbg_suffix = "ing".split("\\|");
    private static final String[] adverb_suffix = "ily|lly|tly".split("\\|");
    private static final String[] adjective_suffix = "able|ible|ful|ic|al|ial|ical|ious|ous|ish|ive|less|y|ly".split("\\|");

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.out.println("Missing arguments.");
            System.out.println("Usage: java Viterbi training_data_path test_data_path output_path");
        }
//        String trainData = "/Users/hming/Documents/NLP/Assignment4/WSJ_POS_CORPUS_FOR_STUDENTS/WSJ_02-21.pos";
//        String testData = "/Users/hming/Documents/NLP/Assignment4/WSJ_POS_CORPUS_FOR_STUDENTS/WSJ_24.words";
        String trainData = args[0];
        String testData = args[1];
        String output = args[2];
        readTrainingData(trainData);
        test(testData, output);
    }

    /*
     * Read the training data.
     * Build the transition probability table and the emission probability table.
     */
    public static void readTrainingData(String trainingData) throws IOException {
        File train = new File(trainingData);
        BufferedReader bf = new BufferedReader(new FileReader(train));
        String line = "";
        List<String> taggedSentence = new ArrayList<>();
        transitionCount.put(START, new HashMap<>());
        while ((line = bf.readLine()) != null) {
            if (line.length() != 0) {
                // update list
                taggedSentence.add(line);
            } else {
                for (int i = 0; i < taggedSentence.size(); i++) {
                    String pair = taggedSentence.get(i);
                    String[] splittedPair = pair.split("\\t");
                    String token = splittedPair[0];
                    String tag = splittedPair[1];

                    // update transition probability table
                    // handle the start of a sentence
                    if (i == 0) {
                        transitionCount.get(START).put(tag, transitionCount.get(START).getOrDefault(tag, 0.0) + 1.0);
                        counter.put(START, counter.getOrDefault(START, 0.0) + 1.0);
                    }
                    if (!transitionCount.containsKey(tag)) transitionCount.put(tag, new HashMap<>());
                    String nextTag = i == taggedSentence.size() - 1 ? END : taggedSentence.get(i + 1).split("\\t")[1];
                    transitionCount.get(tag).put(nextTag, transitionCount.get(tag).getOrDefault(nextTag, 0.0) + 1.0);

                    // update emission probability table
                    if (!emissionCount.containsKey(tag)) {
                        emissionCount.put(tag, new HashMap<>());
                        tags.add(tag);
                    }
                    emissionCount.get(tag).put(token, emissionCount.get(tag).getOrDefault(token, 0.0) + 1.0);

                    // update count of the tag
                    counter.put(tag, counter.getOrDefault(tag, 0.0) + 1.0);
                }
                // clear list
                taggedSentence.clear();
            }
        }

        // convert count to probability and take logrithm of the probability
        for (String s : transitionCount.keySet()) {
            double count = counter.get(s);
            for (String v : transitionCount.get(s).keySet()) {
                transitionCount.get(s).put(v, Math.log10(transitionCount.get(s).get(v) / count));
            }
        }
        for (String s : emissionCount.keySet()) {
            double count = counter.get(s);
            for (String v : emissionCount.get(s).keySet()) {
                emissionCount.get(s).put(v, Math.log10(emissionCount.get(s).get(v) / count));
            }
        }
    }

    /*
     * Test on test data.
     */
    public static void test(String testData, String outputFile) throws IOException {
//        String outputFile = "/Users/hming/Documents/NLP/Assignment4/WSJ_POS_CORPUS_FOR_STUDENTS/My_24.pos";
        File test = new File(testData);
        File output = new File(outputFile);
        BufferedReader bf = new BufferedReader(new FileReader(test));
        BufferedWriter bw = new BufferedWriter(new FileWriter(output));
        String line = "";
        List<String> sentence = new ArrayList<>();
        List<String> predictionTags = new ArrayList<>();
        while ((line = bf.readLine()) != null) {
            if (line.length() != 0) {
                sentence.add(line);
            } else {
                ViterbiAlgo(sentence, predictionTags);
                for (int i = 0; i < sentence.size(); i++) {
                    String word = sentence.get(i);
                    String tag = predictionTags.get(i);
                    if (!emissionCount.get(tag).containsKey(word)) {
                        tag = classifyUnknown(word, tag);
                    }
                    bw.write(word + "\t" + tag);
                    bw.newLine();
                }
                bw.newLine();
                sentence.clear();
                predictionTags.clear();
            }
        }
        bw.close();
    }

    public static void ViterbiAlgo(List<String> sentence, List<String> predictionTags) {
        int numState = tags.size();
        int lenSentence = sentence.size();
        double[][] viterbi = new double[numState + 2][lenSentence];
        int[][] backpointers = new int[numState + 2][lenSentence];
        double unknown = -50.0;

        // initialize viterbi matrix and path matrix
        for (int s = 0; s < numState; s++) {

            double start_to_s_prob = transitionCount.get(START).getOrDefault(tags.get(s), unknown);
            double s_observe_0_prob = emissionCount.get(tags.get(s)).getOrDefault(sentence.get(0), unknown);
            viterbi[s][0] = start_to_s_prob + s_observe_0_prob;
            backpointers[s][0] = 0;
        }

        // viterbi algorithm
        for (int t = 1; t < sentence.size(); t++) {
            for (int s = 0; s < tags.size(); s++) {
                double max = -1000.0;
                double s_observe_t_prob = emissionCount.get(tags.get(s)).getOrDefault(sentence.get(t), unknown);
                for (int arg_s = 0; arg_s < tags.size(); arg_s++) {
                    double prevProb = viterbi[arg_s][t - 1];
                    double arg_s_to_s_prob = transitionCount.get(tags.get(arg_s)).getOrDefault(tags.get(s), unknown);
                    double nextProb = prevProb + arg_s_to_s_prob + s_observe_t_prob;
                    if (nextProb > max) {
                        max = nextProb;
                        viterbi[s][t] = nextProb;
                        backpointers[s][t] = arg_s;
                    }
                }
            }
        }

        // handle end of the sentence
        double max = -1000.0;
        for (int s = 0; s < tags.size(); s++) {
            double prevProb = viterbi[s][lenSentence - 1];
            double s_to_end_prob = transitionCount.get(tags.get(s)).getOrDefault(END, unknown);
            double nextProb = prevProb + s_to_end_prob;
            if (nextProb > max) {
                max = nextProb;
                viterbi[numState][lenSentence - 1] = nextProb;
                backpointers[numState][lenSentence - 1] = s;
            }
        }

        // build backtrace path
        int targetTag = backpointers[numState][lenSentence - 1];
        predictionTags.add(tags.get(targetTag));
        for (int t = lenSentence - 1; t >= 1; t--) {
            targetTag = backpointers[targetTag][t];
            predictionTags.add(tags.get(targetTag));
        }
        Collections.reverse(predictionTags);
    }

    /*
     * Handles unknown words.
     */
    public static String classifyUnknown(String word, String tag) {
        if (Character.isUpperCase(word.charAt(0))) return "NNP";
        if (word.matches(".*[\\d+|\\.].*")) return "CD";
        if (word.contains("$")) return "$";
        if (word.matches(".*[\\+|&|%].*")) return "SYM";
        if (word.contains("-")) return "JJ";
        for (String s : nn_suffix) {
            if (word.endsWith(s)) return "NN";
        }
        for (String s : nns_suffix) {
            if (word.endsWith(s)) return "NNS";
        }
        for (String s : verb_suffix) {
            if (word.endsWith(s)) return "VB";
        }
        for (String s : vbd_suffix) {
            if (word.endsWith(s)) return "VBD";
        }
        for (String s : vbg_suffix) {
            if (word.endsWith(s)) return "VBG";
        }
        for (String s : adverb_suffix) {
            if (word.endsWith(s)) return "RB";
        }
        for (String s : adjective_suffix) {
            if (word.endsWith(s)) return "JJ";
        }

        return tag;
    }
}
