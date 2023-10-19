package com.example.a306cw

class CommentModel {

    private var modelName: String? = null
    private var modelComment : String? = null
    private var modelApproved : Boolean? = false
    private var modelUID: String? = null
    private var modelUserUID: String? = null
    private var modelImage: String? = null

    fun getName(): String {
        return modelName.toString()
    }
    fun setName(name: String?) {
        this.modelName = name
    }

    fun getComment(): String {
        return modelComment.toString()
    }
    fun setComment(comment: String?){
        this.modelComment = comment
    }

    fun getImage(): String? {
        return modelImage
    }
    fun setImage(image_drawable: String) {
        this.modelImage = image_drawable
    }

    fun getApproved(): Boolean? {
        return modelApproved
    }
    fun setApproved(approved: Boolean?){
        this.modelApproved = approved
    }

    fun getUID(): String? {
        return modelUID
    }
    fun setUID(uid: String?){
        this.modelUID = uid
    }

    fun getUserUID(): String? {
        return modelUserUID
    }
    fun setUserUID(uid: String?){
        this.modelUserUID = uid
    }

}