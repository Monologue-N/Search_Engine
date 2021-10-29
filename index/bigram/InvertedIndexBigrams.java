import java.io.IOException;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 * CSCI_572_HW3_InvertedIndexBigrams
 * Student Name: Cancan Hua
 * USC ID: 4612363893
 *
 * Code tweaked from WordCount example on Hadoop website. Below is WordCount example's link:
 * https://hadoop.apache.org/docs/stable/hadoop-mapreduce-client/hadoop-mapreduce-client-core/MapReduceTutorial.html#Example:_WordCount_v1.0
 */

public class InvertedIndexBigrams {

    public static class InvertedIndexMapper extends Mapper<Object, Text, Text, Text> {
        private Text word = new Text();
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            Text docID = new Text();
            // Single ‘\t’ separates the key (Document ID) from the value (Document).
            docID.set(value.toString().split("\t")[0]);
            // convert all to lower case and replace all non-alphabet characters with space
            // for example, "can't" -> "can t"
            String words = value.toString().toLowerCase().replaceAll("[^a-z]+", " ");
            StringTokenizer itr = new StringTokenizer(words);
            String prev = "", curr = "";
            // Each line of the output will be like <word> <docID of doc that this word appears in>
            while (itr.hasMoreTokens()) {
                curr = itr.nextToken();
                if (!prev.isEmpty()) {
                    word.set(prev + " " + curr);
                    context.write(word, docID);
                }
                prev = curr;
            }
        }
    }

    public static class InvertedIndexReducer extends Reducer<Text, Text, Text, Text> {
        private Text res = new Text();
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            // create a hashmap to store docID-frequency pairs
            Map<String, Integer> map = new HashMap<>();
            // traverse docIDs to construct docID-frequency map
            for(Text value: values) {
                String docID = value.toString();
                map.put(docID, map.getOrDefault(docID, 0) + 1);
            }
            StringBuilder docIDFreq = new StringBuilder();
            // construct docID counts after key (word) -> format like: "docID1: count1 docID2: count2"
            for (String docID: map.keySet()) {
                docIDFreq.append(docID).append(":").append(map.get(docID)).append("\t");
            }
            res.set(docIDFreq.toString());
            context.write(key, res);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: Inverted Index <Input_Path> <Output_Path>");
            System.exit(-1);
        }
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Inverted Index Bigrams");
        job.setJarByClass(InvertedIndexBigrams.class);
        job.setMapperClass(InvertedIndexMapper.class);
        job.setReducerClass(InvertedIndexReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}

