import java.io.*;
import java.nio.file.*;
import java.util.*;

public class textClassifier {
    String fileName; 
    int N;  //user input integer N - number of entries in corpus to use as training set 

    String[] stopwords = {" about ", " all ", " along ", " also ", " although ", " among ", " and ", " any ", " anyone ", 
        " anything ", " are ", " around ", " because ", " been ", " before ", " being ", " both ", "but", " came ", " come ", 
        " coming ", " could ", " did ", " each ", " else ", " every ", " for ", " from ", " get ", " getting ", " going ", 
        " got ", " gotten ", " had ", " has ", " have ", " having ", " her ", " here ", " hers ", " him ", " his ", " how ", 
        " however ", " into ", " its ", " like ", " may ",  " most ", " next ", " now ", " only ", " our ", " out ", 
        " particular ", " same ", " she ", " should ", " some ", " take ", " taken ", " taking ", " than ", " that ", " the ", 
        " then ", " there ", " these ", " they ", " this ", " those ", " throughout ", " too ", " took ", " very ", " was ", 
        " went ", " what ", " when "," which ", " while ", " who ", " why ", " will ", " with ", " without ", " would ", " yes ", 
        " yet ", " you ", " your ", " com ", " doc ", " edu ", " encyclopedia ", " fact ", " facts ", " free ", " home ", " htm ", 
        " html ", " http ", " information ", " internet ", " net ", " new ", " news ", " official " , " page ", " pages ", 
        " resource ", " resources ", " pdf ", " site ", " sites ", " usa ", " web ", " wikipedia ", " www ", " one ", " ones ", 
        " two ", " three ", " four ", " five ", " six ", " seven ", " eight ", " nine ", " ten ", " tens ", " eleven ", " twelve ", 
        " dozen ", " dozens ", " thirteen ", " fourteen ", " fifteen ", " sixteen ", " seventeen ", " eighteen ", " nineteen ", 
        " twenty ", " thirty ", " forty ", " fifty ", " sixty ", " seventy ", " eighty ", " ninety ", " hundred ", " hundreds ", 
        " thousand ", " thousands ", " million ", " millions "}; 

    //unique word list - training set 
    ArrayList<String> uniqueWords = new ArrayList<String>(); 

    //testing words 
    ArrayList<String> testingWords = new ArrayList<String>(); 

    //training set <<name, category, biography>, ....> 
    ArrayList<ArrayList<String>> trainingSet = new ArrayList<ArrayList<String>>(); 
    //test set <<name, category, biography>, ....> 
    ArrayList<ArrayList<String>> testingSet = new ArrayList<ArrayList<String>>(); 

    //category -> <biography1, biography2, ....> 
    HashMap<String, ArrayList<String>> category = new HashMap<String, ArrayList<String>>(); 
    //Occ(C): category in training set-> occurences 
    HashMap<String, Integer> categoryOccurences = new HashMap<String, Integer>(); 

    //Occ(W|C): number of biographies that contain word in training set 
        //word -> <category1 count, category2 count, category3 count> 
    HashMap<String, ArrayList<Integer>> OccWC = new HashMap<String, ArrayList<Integer>>(); 

    //Freq(C): fraction of biographies that are of category C in training set 
    HashMap<String, Float> FreqC = new HashMap<String, Float>(); 

    //Freq(W|C): fraction of biographies of category C that contain W in training set 
        //word -> <category1 count, category2 count, category3 count> 
    HashMap<String, ArrayList<Float>> FreqWC = new HashMap<String, ArrayList<Float>>(); 

    //P(C): probabilities for each classification C
    HashMap<String, Float> ProbC = new HashMap<String, Float>(); 

    //P(W|C): probabilities for each word W and category C using Laplacian correction 
    HashMap<String, ArrayList<Float>> ProbWC = new HashMap<String, ArrayList<Float>>(); 

    //L(C): negative log probabilities for each category C 
    HashMap<String, Float> LogC = new HashMap<String, Float>(); 

    //L(W|C): negative log probabiltieis for each word W and category C
    HashMap<String, ArrayList<Float>> LogWC = new HashMap<String, ArrayList<Float>>(); 

    //L(C|B): for test data 
    HashMap<String, ArrayList<Float>> LogCB = new HashMap<String, ArrayList<Float>>(); 

    //actual probabilities - test data 
    //name -> <cat1, cat2, cat3..> 
    HashMap<String, ArrayList<Float>> actualProb = new HashMap<String, ArrayList<Float>>(); 

    //predictions - test data 
    HashMap<String, String> predictions = new HashMap<String, String>(); 

    /*---------------------------------------------------------------------------------------*/
    public static void main(String[] args) throws Exception{
        textClassifier textClassifier = new textClassifier(); 

        //read commnad line 
        textClassifier.fileName = args[0];
        textClassifier.N = Integer.parseInt(args[1]);

        //read file 
        textClassifier.readFile(textClassifier.fileName);

        //learning classifer from training set 
        textClassifier.learning();

        //applying the classifier to the test data 
        textClassifier.applyClassifier(); 

        //output 
        textClassifier.output();

    } 

    /*---------------------------------------------------------------------------------------*/
    void readFile(String fileName) throws Exception{
        Path path = Paths.get(fileName); 
        File file = new File(path.toString()); 

        BufferedReader br = new BufferedReader(new FileReader(file));
        String line = null; 

        //training set -  first N biographies (0 to N-1)
        //testing set - rest of the biographys (>= N)
        //set all to lowercase 
        int count =0; 
        while ((line = br.readLine()) != null){   
            //take into account multiline space between person  
            if (line.length()>0){      
                //read name 
                String name = line.toLowerCase(); 
                //System.out.println(name); 
                //read category 
                String category = br.readLine().toLowerCase(); 
                //System.out.println(category); 
                //read biography 
                StringBuffer stringBuffer = new StringBuffer(""); 
                line = null;  
                while ((line = br.readLine()) != null) {
                    if(line.length() > 0) {
                        //append line to biography 
                        stringBuffer.append(line.replaceAll("[^a-zA-Z ]", "")); 
                    }
                    //multi line space between biographies 
                    else{
                        break;
                    }
                }
                String biography = stringBuffer.toString(); 
                //System.out.println(biography); 
                
                ArrayList<String> newPerson = new ArrayList<String>(); 
                newPerson.add(name); 
                newPerson.add(category); 
                newPerson.add(biography); 

                if (count < N) {
                    //store training set
                    trainingSet.add(newPerson); 
                }
                else{
                    testingSet.add(newPerson);
                }
                count++;
            }
        } 
        /*
        System.out.println("TRAIN " + trainingSet.size());
        for (int i=0; i< trainingSet.size(); i++){
            System.out.println(trainingSet.get(i)); 
            System.out.println(trainingSet.get(i).size());
        }

        System.out.println("TEST "+ testingSet.size());
        for (int i=0; i< testingSet.size(); i++){
            System.out.println(testingSet.get(i)); 
            System.out.println(testingSet.get(i).size());
        }
        */

        //close buffered reader
        br.close(); 
    }
        
    /*---------------------------------------------------------------------------------------*/
    //learning classifer from the training set 
    void learning(){
        //normalization + group biographies by category + category occurences
        categorization(); 

        //create unique word list of words in training set
        uniqueWords();

        //occurence of biographies for each category C that contain word W
        counting(); 
    
        //frequencies 
        frequencies();

        //probabilities 
        probabilities();

        //negative log probabilities
        negativeLogProbabilities();
    }

    /*---------------------------------------------------------------------------------------*/
    //group biographies by categories 
    void categorization(){
        for (int i=0; i< trainingSet.size(); i++){
            //get category 
            String label = trainingSet.get(i).get(1); 

            //normalization - remove stop words in biogrpahy 
            //remove all words of 1/2 letters
            String biography = trainingSet.get(i).get(2).replaceAll("\\b\\w{1,4}\\b","");
            //remove given stopwords 
            for (int j=0; j< stopwords.length; j++){
                if (biography.contains(stopwords[j])){
                    //System.out.println("CONTAINS " + stopwords[j]); 
                    biography = biography.replaceAll(stopwords[j], ""); 
                }
            }

            //unique, add to category
            if (!(category.containsKey(label))){
                ArrayList<String> temp = new ArrayList<String>();  
                //System.out.println("DOES NOT CONTAINS" + label);
                //System.out.println("does not contain label " + label);               
                //add biography 
                temp.add(biography); 
                category.put(label, temp); 
            }
            //not unique, find and add to category 
            else{
                //add to arraylist 
                category.get(label).add(biography); 
                //System.out.println("CONTAINS" + label);
            }
        }
    
        //set category occurences - # of biographies for each category 
        for (Map.Entry<String, ArrayList<String>> entry: category.entrySet()){
            String label = entry.getKey(); 
            ArrayList<String> biographies = entry.getValue(); 
            //System.out.println(label); 
            //System.out.println(biographies); 
            categoryOccurences.put(label, biographies.size()); 
        }
        //System.out.println("categories" + categoryOccurences); 
    }

    /*---------------------------------------------------------------------------------------*/
    //unique word list - training set 
    void uniqueWords(){
        //category 
        for (Map.Entry<String, ArrayList<String>> entry: category.entrySet()){
            ArrayList<String> biographies = entry.getValue(); 
            for (int i=0; i< biographies.size(); i++){
                String line = biographies.get(i);
                //split by space " " to get word
                String[] words = line.split(" "); 
                for (int k=0; k< words.length; k++){
                    //remove duplicates + empty spaces
                    if ((!(uniqueWords.contains(words[k]))) && (words[k].length() > 0 )) {
                        uniqueWords.add(words[k]);
                    }
                }
            }
        }
        /*
        //print unique words list 
        System.out.println(uniqueWords);
        System.out.println(uniqueWords.size());
        */
    }

    /*---------------------------------------------------------------------------------------*/
    //set Occ(W|C) - number of biographies for each category C that contain word W
    void counting(){
        //for each unique word in training set 
        for (int i=0; i< uniqueWords.size(); i++){
            ArrayList<String> tempCategoryCount = new ArrayList<String>(); 

            //initialize category count 
            ArrayList<Integer> tempCount = new ArrayList<Integer>(); 
            for (int j=0; j<category.size(); j++){
                tempCount.add(0); 
            }
        
            int index =0; 
            //for each category
            for (Map.Entry<String, ArrayList<String>> entry: category.entrySet()){
                //get value - biographies for category C 
                //String C = entry.getKey(); 
                ArrayList<String> biographies = entry.getValue(); 
                
                //check how many biographies in category C contains the word 
                    //e.g. writer, gov, music 
                for (int k=0; k< biographies.size(); k++){
                    //if biography contains word 
                    //doesn't matter how many times a word occurs in a given biography
                    if (biographies.get(k).contains(uniqueWords.get(i))){
                        //System.out.println("CONTAINS " + uniqueWords.get(i) + "AT " + index);
                        //word exists, Occ(W|C) +=1 
                        tempCount.set(index, tempCount.get(index)+1);
                    }
                    //otherwise, don't need to add 
                }   
                //increment index for category 
                index++;     
            }
            //set word -> temp count 
            OccWC.put(uniqueWords.get(i), tempCount); 
        }
        /*
        for (Map.Entry<String, ArrayList<Integer>> entry: OccWC.entrySet()){
            System.out.println(entry.getKey()); 
            System.out.println(entry.getValue()); 
        }
        */
    }
    /*---------------------------------------------------------------------------------------*/
    //frequencies 
    void frequencies(){
        //calculate FreqC 
        //T = N (training set size )
        for (Map.Entry<String, Integer> entry: categoryOccurences.entrySet()){
            String label = entry.getKey();
            int val =  entry.getValue(); 
            float freq = (float) val / (float) N; 
            //System.out.println(freq); 
            FreqC.put(label, freq); 
        }
        //System.out.println("FreqC"); 
        //System.out.println(FreqC);  

        //calculate Freq(W|C) 
        //for each word 
        for (Map.Entry<String, ArrayList<Integer>> entry: OccWC.entrySet()){
            //get word 
            String word = entry.getKey(); 

            //initialize arraylist - tempFreq 
            ArrayList<Float> tempFreq = new ArrayList<Float>(); 
            for (int j=0; j<category.size(); j++){
                tempFreq.add((float)0); 
            }

            //for each category 
            for (int i=0; i< entry.getValue().size(); i++){
                //get Occ(W|C) - by word and category 
                int tempOccWC = entry.getValue().get(i); 
                //System.out.println("tempOCCWc " + tempOccWC); 
        
                //get category occurences 
                float tempFreqWC = (float) 0; 
                for (Integer j: categoryOccurences.values()){
                    //same category, search by index 
                    if (i==j){
                        tempFreqWC = (float) tempOccWC/ (float) j; 
                    }
                    //else freqWC is 0
                }

                //set freq in arraylist 
                tempFreq.set(i, tempFreqWC); 
            }
            //add arraylist 
            FreqWC.put(word, tempFreq); 
        }
        //System.out.println("FreqWC");
        //System.out.println(FreqWC);
    }
   
    /*---------------------------------------------------------------------------------------*/
    //probabilities
    void probabilities(){
        //set Laplacian correction 
        float correction = (float) 0.1; 
        int numCategories = categoryOccurences.size(); 

        //calculate P(C)
        for (Map.Entry<String, Float> entry: FreqC.entrySet()){
            String label = entry.getKey(); 
            float tempFreq = entry.getValue(); 
            float tempProb = ((tempFreq + correction) / ((float) (1+numCategories*correction))); 
            ProbC.put(label, tempProb); 
        }
        //System.out.println("ProbC"); 
        //System.out.println(ProbC);  

        //calculate P(W|C)
        //for each word 
        for (Map.Entry<String, ArrayList<Float>> entry: FreqWC.entrySet()){
            //get word 
            String word = entry.getKey(); 

            //initialize arraylist - tempProb
            ArrayList<Float> tempProb = new ArrayList<Float>(); 
            for (int j=0; j<category.size(); j++){
                tempProb.add((float)0); 
            }

            //for each category 
            for (int i=0; i< entry.getValue().size(); i++){
                //get freqWC by word and category 
                float tempFreqWC = entry.getValue().get(i); 
                float tempProbWC = ((tempFreqWC + correction)/ ((float)(1+2*correction))); 
                //set prob in arraylist 
                tempProb.set(i, tempProbWC); 
            }
            //add arraylist 
            ProbWC.put(word, tempProb); 
        }
        //System.out.println("ProbWC");
        //System.out.println(ProbWC);
    }
    /*---------------------------------------------------------------------------------------*/
    //negative log probabilities to avoid underflow
    void negativeLogProbabilities(){
        //use log 2 
        //calculate L(C)
        for (Map.Entry<String, Float> entry: ProbC.entrySet()){
            String label = entry.getKey(); 
            float tempProb = entry.getValue(); 
            //L(C) = -log2P(C)
            float tempLogC = (float) - (Math.log((double) tempProb)/ Math.log((double) 2)); 
            LogC.put(label, tempLogC); 
        }
        //System.out.println("LogC"); 
        //System.out.println(LogC);

        //calculate L(W|C)
         //for each word 
         for (Map.Entry<String, ArrayList<Float>> entry: ProbWC.entrySet()){
            //get word 
            String word = entry.getKey(); 

            //initialize arraylist - tempProb
            ArrayList<Float> tempLog = new ArrayList<Float>(); 
            for (int j=0; j<category.size(); j++){
                tempLog.add((float)0); 
            }

            //for each category 
            for (int i=0; i< entry.getValue().size(); i++){
                //get probWC by word and category 
                float tempProbWC = entry.getValue().get(i); 
                //L(W|C) = -log2(P(W|C))
                float tempLogWC = (float) - (Math.log((double) tempProbWC)/ Math.log((double) 2)); 
                tempLog.set(i, tempLogWC); 
            }
            //add arraylist 
            LogWC.put(word, tempLog); 
        }
        //System.out.println("LogWC");
        //System.out.println(LogWC);
    }

    /*---------------------------------------------------------------------------------------*/
    //applying classifer to the test data
    void applyClassifier(){
        //normalization + calculate L(C|B) 
        computations();

        //prediction 
        predictions(); 

        //recover actual probabilities 
        actualProbabilities(); 

    }
    /*---------------------------------------------------------------------------------------*/
    //normalization + computations of test data 
    void computations(){
        //Hashmap: LogCB <name, <total, total, total > 

        //for each biography 
        for (int i=0; i< testingSet.size(); i++){
            String name = testingSet.get(i).get(0); 
            //remove punctuations 
            String biography = testingSet.get(i).get(2).replaceAll("\\p{Punct}", ""); 
            
            //intailize arraylist - category totals 
            ArrayList<Float> tempTotals = new ArrayList<Float>();
            for (int j=0; j<category.size(); j++){
                tempTotals.add((float)0); 
            }

            //add L(C) 
            int index =0; 
            for (Map.Entry<String, Float> entry: LogC.entrySet()){
                //for each category - get L(C)
                float tempLC = entry.getValue(); 
                tempTotals.set(index, tempLC); 
                index++; 
            }
            //System.out.println("temp totlas " + tempTotals);

            //add L(W|C) for each word in biography 
            //split biography - array of words 
            String[] words = biography.split(" ");
            for (int k=0; k< words.length; k++){
                //normalization
                //skip words of length 1/2 
                if (words[k].length() ==1 || words[k].length() ==2){
                    continue; //skip word - current iteration 
                }
                //skip stopwords
                for (String stopword: stopwords){
                    if (stopword == words[k]){
                        break; 
                    }
                    continue; 
                }
                //skip words not in training set - uniqueWords 
                if (!(uniqueWords.contains(words[k]))){
                    continue; 
                }

                //otherwise- add L(W|C) for each category
                int tempIndex=0; 
                for (int j=0; j< category.size(); j++){
                    float tempLWC = LogWC.get(words[k]).get(tempIndex); 
                    //System.out.println(words[k] + " tempLWC" + tempLWC);
                    tempTotals.set(tempIndex, (tempTotals.get(tempIndex)+tempLWC)); 
                    tempIndex++; 
                }
            }
            //update hashmap 
            LogCB.put(name, tempTotals); 

        }
        System.out.println("LogCB - Log(C|B)"); 
        //System.out.println(LogCB); 
    }
    /*---------------------------------------------------------------------------------------*/
    //smallest value of L(C|B)
    void predictions(){
        //each biography in LogCB
        for (Map.Entry<String, ArrayList<Float>> entry: LogCB.entrySet()){
            String name = entry.getKey(); 
            ArrayList<Float> tempLogCB = entry.getValue(); 
            //get minimum value of L(C|B)
            //float min = Collections.min(tempLogCB); 
            int minIndex = tempLogCB.indexOf(Collections.min(tempLogCB));

            //return category at certain index
            int index = 0; 
            for (Map.Entry<String, ArrayList<String>> cat: category.entrySet()){
                if (index == minIndex){
                    //add corresponding category to prediction
                    predictions.put(name, cat.getKey());
                    break; 
                }
                index++; 
            }
        }
        //System.out.println(predictions); 
    }
    /*---------------------------------------------------------------------------------------*/
    void actualProbabilities(){
        //recover actual probabilities 
        int k = category.size(); 

        //for each biography 
        for (Map.Entry<String, ArrayList<Float>> entry: LogCB.entrySet()){
            String name = entry.getKey(); 
        
            ArrayList<Float>  tempLCB = new ArrayList<Float>(); 
            ArrayList<Float> tempX = new ArrayList<Float>(); 
            ArrayList<Float> tempProb = new ArrayList<Float>(); 
            //store values of L(Ci|B) 

            for (int i=0; i< k; i++){
               tempLCB.add(entry.getValue().get(i)); 
            }
            //find smallest value of L(Ci|B)
            float m = Collections.min((tempLCB)); 
            
            //summation of xi
            float s = 0; 
            for (int i=0; i<k; i++){
                float x; 
                float ci = tempLCB.get(i); 
                //if ci -m < 7 then xi = 2^(m-ci)
                if (ci- m <7){
                    x = (float) Math.pow(2, (m-ci)); 
                }
                //else xi=0 
                else{
                    x= 0; 
                }
                //update s 
                s+= x; 
                //set x value in arraylist 
                tempX.add(x); 
            }

            //probabilities  
            for (int i=0; i< k; i++){
                float p = tempX.get(i) / s; 
                tempProb.add(p); 
            }
            //add to hashmap - actual probabilities 
            actualProb.put(name, tempProb); 
        }
        //System.out.println("actual probabilities")
        //System.out.println(actualProb);
    }

    /*---------------------------------------------------------------------------------------*/
    //output
    void output(){
        //intialize accuracy count 
        int accuracyCount=0; 

        //set categories in array -> index to string 
        String[] categoryArray = new String [category.size()]; 
        int index= 0; 
        for (Map.Entry<String, Integer> entry: categoryOccurences.entrySet()){
            categoryArray[index]= entry.getKey(); 
            index++; 
        }

        //for each biography in testing set
        for (int i=0; i< testingSet.size(); i++){
            //get name 
            String name = testingSet.get(i).get(0); 
            //capitalize + print name
            String[] nameSubstring = name.split(" "); 
            for (int j=0; j< nameSubstring.length; j++){
                System.out.print(nameSubstring[j].substring(0,1).toUpperCase() + nameSubstring[j].substring(1) + ". ");
            }
            System.out.print("  "); 

            //prediction 
            String predicted = predictions.get(name); 
            System.out.print("Prediction: ");
            System.out.print(predicted.substring(0, 1).toUpperCase() + predicted.substring(1) + ". "); 
            System.out.print("  "); 

            //check if actual category is same as predicted -> right or wrong 
            if (predicted.equals(testingSet.get(i).get(1))){
                System.out.println("Right. "); 
                accuracyCount ++; 
            }
            else{
                System.out.println("Wrong. "); 
            }

            //print probabilities for each category
            for (int k=0; k< category.size(); k++){
                //get category name 
                System.out.print(categoryArray[k].substring(0,1).toUpperCase() + categoryArray[k].substring(1) + " : "); 
                //get probabilities: name -> arrayList<float> 
                String formattedProb = String.format("%.02f", actualProb.get(name).get(k));
                System.out.print(formattedProb); 
                System.out.print("   "); 
            }
            System.out.println();
            System.out.println();
        }

        //overall accuracy 
        double accuracy = (double) accuracyCount/ testingSet.size(); 
        String formattedAccuracy = String.format("%.02f", accuracy);
        System.out.print("Overall accuracy: "); 
        System.out.println(accuracyCount + " out of " + testingSet.size()+ " = " + formattedAccuracy);
        System.out.println();
    }

}



