(ns lazr.parse
  (:import java.io.File
           org.apache.commons.imaging.Imaging
           org.apache.commons.imaging.common.bytesource.ByteSourceFile))

(defn format->parser
  [fmt]
  (case fmt
    "PNG" (org.apache.commons.imaging.formats.png.PngImageParser.)
    "JPEG" (org.apache.commons.imaging.formats.jpeg.JpegImageParser.)))

(defn ->image-parser
  [path]
  (-> (File. path)
      (Imaging/guessFormat)
      (.getName)
      (format->parser)))

(defn ->byte-source
  [path]
  (ByteSourceFile. (File. path)))

(defn ->buffered-image
  [parser byte-source]
  (.getBufferedImage parser byte-source nil))
