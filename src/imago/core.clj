(ns imago.core
  (:import [javax.imageio ImageIO]
           [java.io File]
           [java.awt Dimension]
           [javax.swing JFrame JPanel]))

(defn slurp-image [input]
  (let [input (if (instance? String input)
                (File. input)
                input)]
    (ImageIO/read input)))

(defn- max-constraint->scale [original-size max-size]
  (min (if max-size
         (/ max-size original-size)
         1)
       1))

(defn- max-constraint->dimension
  [width height {:keys [max-width max-height]}]
  (let [scale-width (max-constraint->scale width max-width)
        scale-height (max-constraint->scale height max-height)
        scale (min scale-width scale-height)
        new-width (* scale width)
        new-height (* scale height)]
    [new-width new-height]))

(defn- image-panel [image width height]
  (doto (proxy [JPanel] []
          (paintComponent [g]
            (proxy-super paintComponent g)
            (prn [width height])
            (.drawImage g image 0 0 width height nil)))
    (.setPreferredSize (Dimension. width height))))

(defn- image-frame [image width height]
  (let [panel (image-panel image width height)
        frame (JFrame. "Image")]
    (doto frame
      (.setDefaultCloseOperation JFrame/DISPOSE_ON_CLOSE)
      (.setContentPane panel)
      (.pack))))

(defn show-image [image & {:as opts}]
  (let [image-width (.getWidth image nil)
        image-height (.getHeight image nil)
        [width height] (max-constraint->dimension image-width
                                                  image-height
                                                  opts)]
    (doto (image-frame image width height)
      (.setVisible true))))