package com.pokesoft;

public class DecisionTree {
    private class Pair {
        private float value;
        private String category;

        public Pair(float value, String category) {
            this.value = value;
            this.category = category;
        }

        public float getValue() {
            return value;
        }

        public String getCategory() {
            return category;
        }

        public void setValue(float value) {
            this.value = value;
        }

        public void setCategory(String category) {
            this.category = category;
        }
    }
    public void addData(float value, String category){

    }
}
