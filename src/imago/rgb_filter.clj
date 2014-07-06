(ns imago.rgb-filter
  (:gen-class
   :name imago.RGBFilter
   :extends java.awt.image.RGBImageFilter
   :exposes {canFilterIndexColorModel {:get getCanFilterIndexColorModel
                                       :set setCanFilterIndexColorModel}}
   :init init
   :state state
   :constructors {[clojure.lang.IFn Boolean] []}))

(defn -init [filter-fn cficm]
  (prn "!!")
;  (.setCanFilterIndexColorModel cficm)
  [[] filter-fn])

(defn -filterRGB [this x y rgb]
  ((.state this) x y rgb))
