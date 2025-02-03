function like(obj,entityType,entityId,entityUserId,postId) {
    $.post(
        PROJECT_ROOT + "/like/giveLike",
        {"entityType":entityType,"entityId":entityId,"entityUserId":entityUserId,"postId":postId},
        function (data) {
            data = $.parseJSON(data);
            if(data.code==0){
                $(obj).children("i").text(data.likeCount);
                $(obj).children("b").text(data.likeStatus==1?'已赞':'赞');
            }else{
                alert(data.msg);
            }
        }
    )
}

$(function () {
    $("#topBtn").click(setTop);
    $("#fineBtn").click(setFine);
    $("#deleteBtn").click(setDelete);
});

function setTop() {
    $.post(
        PROJECT_ROOT + "/discuss/top",
        {"id":$("#postId").val()},
        function (data) {
            data = $.parseJSON(data);
            if(data.code == 0){
                $("#topBtn").attr("disabled","disabled");
            }else{
                alert(data.msg);
            }
        }
    );
}

function setFine() {
    $.post(
        PROJECT_ROOT + "/discuss/fine",
        {"id":$("#postId").val()},
        function (data) {
            data = $.parseJSON(data);
            if(data.code == 0){
                $("#fineBtn").attr("disabled","disabled");
            }else{
                alert(data.msg);
            }
        }
    );
}

function setDelete() {
    $.post(
        PROJECT_ROOT + "/discuss/delete",
        {"id":$("#postId").val()},
        function (data) {
            data = $.parseJSON(data);
            if(data.code == 0){
                // $("#deleteBtn").attr("disabled","disabled");
                window.location.href=PROJECT_ROOT+"/index";
            }else{
                alert(data.msg);
            }
        }
    );
}