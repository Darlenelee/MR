package sjtu.sdic.mapreduce.core;

import com.alibaba.fastjson.*;
import sjtu.sdic.mapreduce.common.KeyValue;
import sjtu.sdic.mapreduce.common.Utils;
import sun.awt.SunHints;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static sjtu.sdic.mapreduce.WordCount.mapFunc;
import static sjtu.sdic.mapreduce.common.Utils.*;

/**
 * Created by Cachhe on 2019/4/19.
 */
public class Mapper {

    /**
     * doMap manages one map task: it should read one of the input files
     * {@code inFile}, call the user-defined map function {@code mapFunc} for
     * that file's contents, and partition mapFunc's output into {@code nReduce}
     * intermediate files.
     *
     * There is one intermediate file per reduce task. The file name
     * includes both the map task number and the reduce task number. Use
     * the filename generated by {@link Utils#reduceName(String, int, int)}
     * as the intermediate file for reduce task r. Call
     * {@link Mapper#hashCode(String)} on each key, mod nReduce,
     * to pick r for a key/value pair.
     *
     * {@code mapFunc} is the map function provided by the application. The first
     * argument should be the input file name, though the map function
     * typically ignores it. The second argument should be the entire
     * input file contents. {@code mapFunc} returns a list containing the
     * key/value pairs for reduce; see {@link KeyValue} for the definition of
     * KeyValue.
     *
     * Look at Java's File and Files API for functions to read
     * and write files.
     *
     * Coming up with a scheme for how to format the key/value pairs on
     * disk can be tricky, especially when taking into account that both
     * keys and values could contain newlines, quotes, and any other
     * character you can think of.
     *
     * One format often used for serializing data to a byte stream that the
     * other end can correctly reconstruct is JSON. You are not required to
     * use JSON, but as the output of the reduce tasks *must* be JSON,
     * familiarizing yourself with it here may prove useful. There're many
     * JSON-lib for Java, and we recommend and supply with FastJSON powered by
     * Alibaba. You can refer to official docs or other resources to figure
     * how to use it.
     *
     * The corresponding decoding functions can be found in {@link Reducer}.
     *
     * Remember to close the file after you have written all the values!
     *
     * Your code here (Part I).
     *
     * @param jobName the name of the MapReduce job
     * @param mapTask which map task this is
     * @param inFile file name (if in same dir, it's also the file path)
     * @param nReduce the number of reduce task that will be run ("R" in the paper)
     * @param mapFunc the user-defined map function
     */
    public static void doMap(String jobName, int mapTask, String inFile, int nReduce, MapFunc mapFunc) {
        /**
         * inputContent, err := ioutil.ReadFile(inFile)
         *     if err != nil {
         *         panic(err)
         *     }
         */
        List<File> files = new ArrayList<File>();
        List<FileOutputStream> fos= new ArrayList<FileOutputStream>();
        List<JSONArray> list = new ArrayList<JSONArray>();
        File infile = new File(inFile);
        FileReader fr = null;
        try {
            fr = new FileReader(infile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        char[] content = new char[Integer.parseInt(String.valueOf(infile.length()))];
        try {
            fr.read(content);
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /**
         *     keyValues := mapF(inFile, string(inputContent))
         */
        List<KeyValue> pair = mapFunc.map(inFile, String.valueOf(content));
        /**
         *  var intermediateFileEncoders []*json.Encoder
         *     for reduceTaskNumber := 0; reduceTaskNumber < nReduce; reduceTaskNumber++ {
         *         intermediateFile, err := os.Create(reduceName(jobName, mapTask, reduceTaskNumber))
         *         if err != nil {
         *             panic(err)
         *         }
         *         defer intermediateFile.Close()
         *         enc := json.NewEncoder(intermediateFile)
         *         intermediateFileEncoders = append(intermediateFileEncoders, enc)
         *     }
         */
        for (int i=0;i<nReduce;i++){
            try {
                files.add(new File(reduceName(jobName, mapTask, i)));
                fos.add(i, new FileOutputStream(files.get(i)));
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
            list.add(new JSONArray());
        }
        /**
         * for _, kv := range keyValues {
         *         err := intermediateFileEncoders[ihash(kv.Key) % nReduce].Encode(kv)
         *         if err != nil {
         *             panic(err)
         *         }
         *     }
         */
        for(KeyValue item : pair)    {
            int basket = hashCode(item.key) % nReduce;
            JSONArray filearray = list.get(basket);
            filearray.add(item);
            list.set(basket, filearray);
            if(debugEnabled)
                System.out.println("Log: key:"+item.key+"\tvalue:"+item.value+"\n");

        }
        for (int i=0;i<nReduce;i++){
            try {
                JSON.writeJSONString(fos.get(i), list.get(i));
                fos.get(i).close();
                } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


    /**
     * a simple method limiting hash code to be positive
     *
     * @param src string
     * @return a positive hash code
     */
    private static int hashCode(String src) {
        return src.hashCode() & Integer.MAX_VALUE;
    }

}
