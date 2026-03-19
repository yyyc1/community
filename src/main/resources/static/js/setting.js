$(function(){
    $("#uploadFormBtn").click(upload);
    bsCustomFileInput.init();
    $("#savePwdBtn").click(function() {
        // 1. 前端先做基础校验（避免无效请求）
        const oldPwd = $("#old-password").val().trim();
        const newPwd = $("#new-password").val().trim();
        const confirmPwd = $("#confirm-password").val().trim();
        const errorTip = $("#errorTip");

        // 清空之前的错误提示
        errorTip.addClass("d-none").text("");
        // 重置表单的原生校验样式
        $("#passwordForm")[0].classList.remove("was-validated");

        // 校验规则
        if (newPwd.length < 8) {
            errorTip.text("密码长度不能小于8位!").removeClass("d-none");
            return;
        }
        if (newPwd !== confirmPwd) {
            errorTip.text("两次输入的新密码不一致!").removeClass("d-none");
            return;
        }

        // 2. AJAX 异步提交数据到后端
        $.ajax({
            url:CONTEXT_PATH + "/user/updatePassword", // 后端接口地址（和你原来的 th:action 一致）
            type: "POST",
            data: {
                oldPassword: oldPwd,
                newPassword: newPwd
            },
            dataType: "json", // 期望后端返回 JSON 格式
            success: function(res) {
                if (res.success) {
                    // 成功提示：可跳转到登录页/提示后刷新
                    errorTip.removeClass("alert-danger").addClass("alert-success");
                    errorTip.text("密码修改成功，请重新登录!").removeClass("d-none");
                    // 可选：3秒后跳转到登录页
                    setTimeout(() => {
                        window.location.href = CONTEXT_PATH + "/login";
                    }, 3000);
                } else {
                    // 失败提示（如原密码错误）
                    errorTip.text(res.msg || "修改失败，请重试!").removeClass("d-none");
                }
            },
            error: function() {
                // 网络/服务器错误提示
                errorTip.text("服务器异常，请稍后再试!").removeClass("d-none");
            }
        });
    });

    // 可选：表单原生校验（点击按钮时触发）
    $("#passwordForm").on("submit", function(e) {
        e.preventDefault(); // 阻止原生提交
        $(this).addClass("was-validated");
    });
});

function upload() {
    const fileName = $("#resource_key").val();
    if (!fileName) {
        alert("文件标识为空，请刷新页面重试");
        return;
    }

    const loadingHtml = `
        <div id="uploadLoading" style="position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); z-index: 9999; display: flex; justify-content: center; align-items: center; color: #fff; font-size: 16px;">
            上传中，请稍候...
        </div>
    `;
    $("body").append(loadingHtml);

    $.get(CONTEXT_PATH + "/user/getUploadToken/" + fileName, function (res){
        if(res.code !== 0) {
            alert(res.msg);
            return;
        }
        const  formData = new FormData($("#uploadForm")[0]);
        formData.append("token",res.uploadToken);
        $.ajax({
            url: "http://upload-z0.qiniup.com",
            method: "post",
            processData: false,
            contentType: false,
            data: formData,
            dataType: "json",
            success: function(data) {
                if(data && data.code === 0) {
                    // 更新头像访问路径
                    $.post(
                        CONTEXT_PATH + "/user/updateHeaderUrl/" + $('#resource_key').val(),
                        function(data) {
                            data = $.parseJSON(data);
                            if(data.code === 0) {
                                const qiniuDomain = "http://t95i2j7bk.hd-bkt.clouddn.com/";
                                const avatarUrl = qiniuDomain + $('#resource_key').val() + "?t=" + new Date().getTime();
                                $("#navbarDropdown img.rounded-circle").attr("src", avatarUrl);
                                alert("头像更新成功！");
                            } else {
                                alert(data.msg);
                            }
                        }
                    );
                } else {
                    alert("上传失败!");
                }
            },
            error: function () {
                alert("文件上传请求失败，请检查网络");
            },
            complete: function () {
                // complete无论成功/失败都会执行，统一移除上传中提示，避免漏关
                $("#uploadLoading").remove();
            }
        });
    },"json").fail(function (){
        alert("网络错误，无法获取上传凭证");
    });
}
