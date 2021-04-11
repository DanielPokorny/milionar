package com.pokesoft;

public class Config {
    public Storage storage;
    public class Storage {
        public String getMatchesPath() {
            return matchesPath;
        }

        public void setMatchesPath(String matchesPath) {
            this.matchesPath = matchesPath;
        }

        public String getResultsPath() {
            return resultsPath;
        }

        public void setResultsPath(String resultsPath) {
            this.resultsPath = resultsPath;
        }

        private String matchesPath;
        private String resultsPath;
    }
}
