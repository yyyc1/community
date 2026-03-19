$(function() {


    $("#sendCodeBtn").click(function() {

        var sendCodeBtn = $(this);
        var email = $("#your-email").val().trim();
        // 前端简单校验邮箱格式
        if (!email) {
            alert("请输入邮箱！");
            return;
        }
        var reg = /^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/;
        if (!reg.test(email)) {
            alert("请输入正确的邮箱格式！");
            return;
        }

        // 禁用按钮，防止重复点击（先临时禁用，避免多次请求）
        sendCodeBtn.prop("disabled", true).text("发送中...").addClass("disabled");
        // 异步请求后端接口
        $.ajax({
            url: CONTEXT_PATH + "/sendForgetEmail", // 后端Controller的接口地址
            type: "POST", // 建议用POST，更安全
            data: { email: email }, // 传递给后端的参数：用户输入的邮箱
            dataType: "json", // 期望后端返回JSON格式
            success: function(res) {
                // 后端返回成功（假设res.code=0代表成功）
                if (res.code === 0) {
                    alert(res.msg || "验证码发送成功，请注意查收！");
                    // 开启倒计时（60秒常用）
                    sendCodeBtn.prop("disabled", true).addClass("disabled");
                    countDown(sendCodeBtn);
                } else {
                    // 后端返回业务失败（如邮箱不存在、发送超限）
                    alert(res.msg || "验证码发送失败！");
                    // 恢复按钮可用
                    sendCodeBtn.prop("disabled", false).text("获取验证码").removeClass("disabled");
                }
            },
            error: function() {
                // 网络错误、接口报错等异常
                alert("网络异常，请稍后重试！");
                sendCodeBtn.prop("disabled", false).text("获取验证码").removeClass("disabled");
            }
        });
    });


    $("#doForgetBtn").click(function() {
        var doForgetBtn = $(this);
        var email = $("#your-email").val().trim();
        var password = $("#your-password").val().trim();
        var confirmPwd = $("#confirmPassword").val().trim();
        var inputCode = $("#verifyCode").val().trim();
        if (!email) {
            alert("请输入邮箱！");
            return;
        }
        var reg = /^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/;
        if (!reg.test(email)) {
            alert("请输入正确的邮箱格式");
            return;
        }
        if (password.length < 8) {
            alert("密码长度不能小于8位");
            return;
        }
        if (password !== confirmPwd ) {
            alert("两次密码不一致");
            return;
        }
        $.ajax({
            url: CONTEXT_PATH + "/doForget", // 后端Controller的接口地址
            type: "POST", // 建议用POST，更安全
            data: {
                email: email,
                password: password,
                inputCode: inputCode
            }, // 传递给后端的参数：用户输入的邮箱
            dataType: "json", // 期望后端返回JSON格式
            success: function(res) {
                if(res.code === 0){
                    alert("操作成功！");
                    setTimeout(function() {
                        window.location.href = CONTEXT_PATH + "/login";
                    }, 1500);
                }else{
                    alert(res.msg);
                }
            },
            error: function() {
                alert("网络异常");

            }
        });
    });



    // 倒计时函数：按钮禁用count秒，显示倒计时
    function countDown(btn) {
        var count = 60;
        btn.text(count + "秒后重新获取");
        var timer = setInterval(function() {
            count--;
            btn.text(count + "秒后重新获取");
            if (count <= 0) {
                clearInterval(timer); // 清除定时器
                btn.prop("disabled", false).text("获取验证码").removeClass("disabled");
            }
        }, 1000);
    }
});