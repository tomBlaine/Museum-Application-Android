package com.example.a306cw

class ArtefactModel {
    private var modelName: String? = null
    private var modelImage: String? = null
    var modelYear: String? = null
    private var modelUID: String? = null

    fun getName(): String {
        return modelName.toString()
    }
    fun setName(name: String) {
        this.modelName = name
    }
    fun getImage(): String? {
        return modelImage
    }
    fun setImage(image_drawable: String) {
        this.modelImage = image_drawable
    }
    fun getYear(): String?{
        return  modelYear
    }
    fun setYear(year: String?){
        this.modelYear = year
    }
    fun getUID(): String?{
        return modelUID
    }
    fun setUID(uid: String?){
        this.modelUID = uid
    }

}
