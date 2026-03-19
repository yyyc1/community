function refresh_captcha(){

    $.ajax({
        url: CONTEXT_PATH + "/captcha?p=" + Math.random(),
        type: "GET",
        dataType: "json",
        success: function(res) {
            if (res.code === 0) {
                $("#captchaKey").val(res.captchaKey);
                $("#captcha").attr("src",res.base64Image);
            }
        },
        error: function() {
            alert("网络异常");

        }
    });

}
$(function() {
    refresh_captcha();
});