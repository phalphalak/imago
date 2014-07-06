(ns imago.core
  (:import [javax.imageio ImageIO]
           [java.io File]
           [java.awt Dimension Toolkit]
           [javax.swing JFrame JPanel]
           [imago RGBFilter]
           [java.awt.image FilteredImageSource]
           ))

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

(defprotocol ImageFilter
  (filter-image [this image]))

(def toolkit (Toolkit/getDefaultToolkit))

(extend-type RGBFilter
  ImageFilter
  (filter-image [this image]
    (->> (FilteredImageSource. (.getSource image) this)
         (.createImage toolkit))))

(defn rgb-image-filter [filter-fn can-filter-index-color-model]
  (RGBFilter. filter-fn can-filter-index-color-model))

(defn compose-filters
  ([& filters]
     (if (= 1 (count filters))
       (first filters)
       (fn [x y colour]
         ((apply comp
                 (map #(partial % x y)
                      filters))
          colour)))))

(defn filter [img & filter-fns]
  {:pre [(pos? (count filter-fns))]}
  (filter-image (rgb-image-filter (apply compose-filters filter-fns) true) img))

(def red-mask 0xff0000)
(def green-mask 0x00ff00)
(def blue-mask 0x0000ff)
(def alpha-mask 0xff000000)

(defn int->red [rgba]
  (bit-and (bit-shift-right rgba 16)
           0xff))

(defn int->green [rgba]
  (bit-and (bit-shift-right rgba 8)
           0xff))

(defn int->blue [rgba]
  (bit-and rgba 0xff))

(defn int->alpha [rgba]
  (bit-and (bit-shift-right rgba 24)
           0xff))

(defn int->rgba [rgba]
  ((juxt int->red int->green int->blue int->alpha) rgba))

(defn rgba [r g b a]
  (bit-or (bit-shift-left a 24)
          (bit-shift-left r 16)
          (bit-shift-left g 8)
          b))

#_(defn scale-channel [v scale]
  (let [ratio (1 - scale)]))

(defn mask-filter [mask]
  (fn [_ _ rgb]
    (bit-and rgb mask)))

(def red-filter
  (mask-filter (bit-or red-mask alpha-mask)))

(def green-filter
  (mask-filter (bit-or green-mask alpha-mask)))

(def blue-filter
  (mask-filter (bit-or blue-mask alpha-mask)))

(defn normalize [n]
  (min (max (int n) 0) 255))

(defn gray-scale-filter
  ([] (gray-scale-filter 1 1 1))
  ([red-weight green-weight blue-weight]
     (let [weight-sum (+ red-weight green-weight blue-weight)]
       (fn [_ _ colour]
         (let [[r g b a] (int->rgba colour)]
           (-> (/ (+ (* red-weight r)
                     (* green-weight g)
                     (* blue-weight b))
                  weight-sum)
               (normalize)
               (#(rgba % % % a))))))))

(defn gray-scale-ntsc-filter []
  (gray-scale-filter 0.299 0.587 0.114))
