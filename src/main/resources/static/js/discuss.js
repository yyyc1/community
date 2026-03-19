$.ajaxSetup({
    xhrFields: {
        withCredentials: true // 核心：允许携带跨域凭证（Session-Cookie）
    },
    crossDomain: true // 配合withCredentials，兼容跨域场景（即使同域也加，无副作用）
});


$(function () {
    // 全局提示函数（可替换为layer/ElementUI等组件提示）
    function showMsg(msg, isSuccess = true) {
        if (isSuccess) {
            alert("成功：" + msg);
        } else {
            alert("失败：" + msg);
        }
    }

    // 1. 置顶/取消置顶按钮点击事件
    $("#topBtn").on("click", function () {
        const $this = $(this);
        const postId = $this.data("post-id");
        const currentType = $this.data("type");
        const targetType = currentType === 1 ? 0 : 1;
        // 保存原按钮文字，用于异常时恢复
        const originalText = $this.text();

        if ($this.hasClass("disabled")) return;
        $this.addClass("disabled").text("处理中...");

        $.ajax({
            url: CONTEXT_PATH + "/discuss/top",
            type: "POST",
            xhrFields: {
                withCredentials: true // 允许跨域携带Cookie，同域也需要！
            },
            crossDomain: true, // 配合withCredentials，确保Cookie正常传递
            data: {
                postId: postId,
                type: targetType
            },
            dataType: "json",
            success: function (res) {
                if (res.code === 0) { // 适配后端的code字段，不是success！
                    if (targetType === 1) {
                        $this.removeClass("btn-danger").addClass("btn-secondary");
                        $this.text("取消置顶");
                    } else {
                        $this.removeClass("btn-secondary").addClass("btn-danger").text("置顶");
                    }
                    $this.data("type", targetType);
                    showMsg(targetType === 1 ? "置顶成功" : "取消置顶成功"); // 直接展示后端返回的提示
                } else {
                    showMsg("置顶操作失败", false);
                }
            },
            error: function (xhr) {
                showMsg("网络错误或服务器异常", false);
                console.error("请求异常：", xhr);
            },
            complete: function () {
                $this.removeClass("disabled");
            }
        });
    });

    // 2. 加精/取消加精按钮点击事件
    $("#wonderfulBtn").on("click", function () {
        const $this = $(this);
        const postId = $this.data("post-id");
        // 从按钮文字判断当前状态（兼容未加data-status的情况）
        const currentStatus = $this.data("post-status");
        const targetStatus = currentStatus === 1 ? 0 : 1;


        if ($this.hasClass("disabled")) return;
        $this.addClass("disabled").text("处理中...");

        $.ajax({
            url: CONTEXT_PATH + "/discuss/wonderful",
            type: "POST",
            xhrFields: {
                withCredentials: true // 允许跨域携带Cookie，同域也需要！
            },
            crossDomain: true,
            data: {
                postId: postId,
                status: targetStatus
            },
            dataType: "json",
            success: function (res) {
                if (res.code === 0) {
                    if (targetStatus === 1) {
                        $this.removeClass("btn-danger").addClass("btn-secondary").text("取消加精");
                    } else {
                        $this.removeClass("btn-secondary").addClass("btn-danger").text("加精");
                    }
                    $this.data("status", targetStatus);
                    showMsg(targetType === 1 ? "加精成功" : "取消加精成功");
                } else {
                    showMsg("加精操作失败", false);
                }
            },
            error: function (xhr) {
                showMsg("网络错误/服务器超时", false);
                console.error("加精请求异常：", xhr);
            },
            complete: function () {
                $this.removeClass("disabled");
            }
        });
    });

    // 3. 删除/恢复帖子按钮（加二次确认，适配admin权限）
    $("#deleteBtn").on("click", function () {
        const $this = $(this);
        const postId = $this.data("post-id");
        const currentStatus = $this.data("status");
        const targetStatus = currentStatus === 2 ? 0 : 2;
        const originalText = $this.text();
        const confirmMsg = targetStatus === 2 ? "确定删除该帖子吗？删除后可恢复" : "确定恢复该帖子吗？";

        // 二次确认，防止误操作
        if (!confirm(confirmMsg)) return;
        if ($this.hasClass("disabled")) return;
        $this.addClass("disabled").text("处理中...");

        $.ajax({
            url: CONTEXT_PATH + "/discuss/delete",
            type: "POST",
            xhrFields: {
                withCredentials: true // 允许跨域携带Cookie，同域也需要！
            },
            crossDomain: true,
            data: {
                postId: postId,
                status: targetStatus
            },
            dataType: "json",
            success: function (res) {
                if (res.code === 0) {
                    if (targetStatus === 2) {
                        $this.removeClass("btn-danger").addClass("btn-info");
                        $this.text("恢复帖子");
                    } else {
                        $this.removeClass("btn-info").addClass("btn-danger");
                        $this.text("删除帖子");
                    }
                    $this.data("status", targetStatus);
                    showMsg(res.msg);
                } else {
                    showMsg(res.msg || "帖子操作失败", false);
                }
            },
            error: function (xhr) {
                if (xhr.status === 401 || xhr.status === 302) {
                    showMsg("登录状态失效，请重新登录", false);
                } else if (xhr.status === 403) {
                    showMsg("仅管理员可执行该操作", false);
                } else {
                    showMsg("网络错误/服务器超时", false);
                }
                console.error("删除/恢复请求异常：", xhr);
            },
            complete: function () {
                $this.removeClass("disabled");
                $this.text(originalText);
            }
        });
    });

});

function like(btn, entityType, entityId, entityUserId, postId) {
    $.post(
        CONTEXT_PATH + "/like",
        {"entityType":entityType,"entityId":entityId,"entityUserId":entityUserId,"postId":postId},
        function(data) {
            data = $.parseJSON(data);
            if(data.code == 0) {
                $(btn).children("i").text(data.likeCount);
                $(btn).children("b").text(data.likeStatus==1?'已赞':"赞");
            } else {
                alert(data.msg);
            }
        }
    );
}

