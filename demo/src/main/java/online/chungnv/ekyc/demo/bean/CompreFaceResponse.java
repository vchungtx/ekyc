package online.chungnv.ekyc.demo.bean;

import java.util.List;

public class CompreFaceResponse {
    List<CompreFaceResponseResult> result;

    public List<CompreFaceResponseResult> getResult() {
        return result;
    }

    public void setResult(List<CompreFaceResponseResult> result) {
        this.result = result;
    }

    public static class CompreFaceResponseResult{
        List<CompreFaceResponseResultFaceMatches> face_matches;

        public List<CompreFaceResponseResultFaceMatches> getFace_matches() {
            return face_matches;
        }

        public void setFace_matches(List<CompreFaceResponseResultFaceMatches> face_matches) {
            this.face_matches = face_matches;
        }
    }

    public static class CompreFaceResponseResultFaceMatches{
        float similarity;

        public float getSimilarity() {
            return similarity;
        }

        public void setSimilarity(float similarity) {
            this.similarity = similarity;
        }
    }
}
