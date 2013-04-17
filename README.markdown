# pdcstr

pdcstr is a simple podcast download tool

## Usage

    lein uberjar
    java -jar target/pdcstr.jar /path/to/configuration.edn /path/to/destination/folder 

## Sample configuration

    [{:name "Wait Wait Dont Tell Me"
      :url "http://www.npr.org/rss/podcast.php?id=35"
      :max-files 1}
     {:name "This American Life"
      :url "http://feeds.thisamericanlife.org/talpodcast"}
     {:name "The Talk Show"
      :url "http://feeds.feedburner.com/the_talk_show"
      :max-files 1}]
