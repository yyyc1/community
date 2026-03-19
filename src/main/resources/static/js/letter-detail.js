var scrollContainer; // 滚动容器
var conversationId; // 会话ID
var currentUserId; // 当前登录用户ID
var pageSize = 10; // 每次加载条数（与后端保持一致，初始化10条，懒加载可改为20条）
var currentLatestLetterId; // 已加载消息的最小ID（用于懒加载查询更早消息）
var hasMore = true; // 是否还有更多历史消息
var loading = false; // 加载标记位：防止重复请求



/**
 * 页面DOM加载完成后执行（入口函数）
 */
$(function() {
    // 1. 初始化全局变量，获取页面隐藏域和DOM元素
    initGlobalVars();

    // 2. 初始化处理：提取首批消息的最小ID，滚动到底部显示最新消息
    initFirstBatchMessage();

    // 3. 绑定滚动事件：监听滚动到顶部，触发懒加载
    bindScrollEvent();
});


function initGlobalVars() {
    scrollContainer = $(".message-scroll-container");
    conversationId = $("#conversationId").val();
    currentUserId = $("#currentUserId").val();
}



function initFirstBatchMessage() {
    var messageList = $("#messageList");
    var firstBatchMsgItems = messageList.find("li.media");

    // 2.1 提取首批消息的最小ID（用于后续懒加载，按id从小到大排序，第一条即为最小ID）
    if (firstBatchMsgItems.length > 0) {
        // 获取第一条消息的data-msg-id属性（后端渲染时绑定的lvo.letter.id）
        currentLatestLetterId = firstBatchMsgItems.first().data("msg-id");
        // 标记有更多数据（后续可通过懒加载验证）
        hasMore = true;
    } else {
        // 无首批消息，标记无更多数据
        currentLatestLetterId = 0;
        hasMore = false;
    }

    // 2.2 滚动到底部，默认显示最新消息（首批消息的最后一条）
    scrollContainer.scrollTop(scrollContainer[0].scrollHeight);
}

function bindScrollEvent() {
    scrollContainer.scroll(function() {

        console.log("scrollTop:", scrollContainer.scrollTop());
        console.log("hasMore:", hasMore);
        console.log("loading:", loading);

        // 判断条件：1. 滚动到距离顶部50px以内 2. 还有更多数据 3. 未在加载中
        if (scrollContainer.scrollTop() < 50 && hasMore && !loading) {
            console.log("滚动到顶部，触发懒加载更早历史消息...");
            // 执行懒加载方法
            loadMoreHistoryMessage();
        }
    });
}

function loadMoreHistoryMessage() {
    // 4.1 标记为加载中，防止重复请求
    loading = true;

    // 4.2 记录加载前的容器高度（用于后续重置滚动位置，保证流畅性）
    var preLoadHeight = scrollContainer[0].scrollHeight;

    // 4.3 异步请求后端懒加载接口（POST请求，传递核心参数）
    $.post(
        // 后端懒加载接口地址（根据你的项目实际路径调整）
        CONTEXT_PATH + "/letter/loadMoreHistoryLetter",
        // 传递给后端的参数（与后端接口@Param注解对应）
        {
            conversationId: conversationId,
            latestLetterId: currentLatestLetterId,
            pageSize: pageSize
        },
        // 回调函数：处理后端返回结果
        function(data) {
            // 4.4 解析后端返回的JSON数据（后端用CommunityUtil.getJSONString封装）
            var result = JSON.parse(data);
            if (result.code === 0 && result.letterList  && result.letterList .length > 0) {
                // 4.5 成功获取数据：渲染更早的历史消息（插入到列表顶部）
                renderMoreHistoryMessage(result.letterList);

                // 4.6 更新核心状态变量
                // 更新最小ID：本次加载的消息列表中，第一条是更早的消息（最小ID）
                currentLatestLetterId = result.letterList[0].letter.id;
                // 更新是否还有更多数据（后端返回的hasMore）
                hasMore = result.hasMore;

                // 4.7 重置滚动位置，避免滚动条突然跳转（提升用户体验）
                var postLoadHeight = scrollContainer[0].scrollHeight;
                var addedHeight = postLoadHeight - preLoadHeight;
                // 保留50px偏移，方便用户继续滚动加载下一批
                scrollContainer.scrollTop(addedHeight + 50);
            } else {
                // 4.8 无更多历史消息
                hasMore = false;
                console.log("已加载所有历史消息，无更多数据");
            }

            // 4.9 标记为加载完成，允许下次请求
            loading = false;
        }
    ).fail(function() {
        // 4.10 请求失败处理
        console.error("懒加载历史消息请求失败");
        loading = false;
        hasMore = false;
    });
}

/**
 * 步骤5：渲染懒加载获取的更早历史消息（插入到消息列表顶部）
 * @param messageList 后端返回的LetterVo列表
 */
function renderMoreHistoryMessage(messageList) {
    var messageUl = $("#messageList");
    var newHtml = "";

    // 5.1 遍历消息列表，组装HTML（复刻后端初始化的消息结构，保证样式统一）
    for (var i = 0; i < messageList.length; i++) {
        var lvo = messageList[i];
        var isMyMessage = lvo.letter.fromId === Number(currentUserId);;
        var msgId = lvo.letter.id;
        var username = lvo.fromUser.username;
        var headerUrl = lvo.fromUser.headerUrl;
        var createTime = new Date(lvo.letter.createTime).toLocaleString();
        var content = lvo.letter.content;

        // 组装单条消息HTML（与后端Thymeleaf渲染结构完全一致）
        newHtml += '<li class="media pb-3 pt-3 mb-2" data-msg-id="' + msgId + '">' +
            (isMyMessage ? '' : '<a href="profile.html"><img src="' + headerUrl + '" class="mr-4 rounded-circle user-header" alt="用户头像" ></a>') +
            '<div class="toast show d-lg-block ' + (isMyMessage ? 'toast-right' : 'toast-left') + '" role="alert" aria-live="assertive" aria-atomic="true">' +
            '<div class="toast-header">' +
            '<strong class="mr-auto">' + username + '</strong>' +
            '<small>' + createTime + '</small>' +
            '<button type="button" class="ml-2 mb-1 close" data-dismiss="toast" aria-label="Close">' +
            '<span aria-hidden="true">&times;</span>' +
            '</button>' +
            '</div>' +
            '<div class="toast-body">' + content + '</div>' +
            '</div>' +
            (isMyMessage ? '<a href="profile.html"><img src="' + headerUrl + '" class="ml-4 rounded-circle user-header" alt="用户头像" ></a>' : '') +
            '</li>';
    }

    // 5.2 插入到消息列表顶部（保持消息时序：顶部更早，底部更新）
    messageUl.prepend(newHtml);
}

$(function(){
	$("#sendBtn").click(send_letter);
	$(".close").click(delete_msg);
});

function send_letter() {
    $("#sendModal").modal("hide");

    var targetId = $("#recipient-name").val();
    var content = $("#message-text").val();

    // 前置校验：避免空内容提交
    if ($.trim(content) === "") {
        $("#hintBody").text("请输入私信内容！");
        $("#hintModal").modal("show");
        setTimeout(function(){
            $("#hintModal").modal("hide");
        }, 2000);
        return;
    }

    $.post(
        CONTEXT_PATH + "/letter/send",
        {"targetId":targetId,"content":content},
        function(data) {
            data = $.parseJSON(data);
            if(data.code === 0) {
                $("#hintBody").text("发送成功!");

                // 【核心优化】：无刷新追加新消息到列表底部（与懒加载无缝衔接）
                appendNewMessageToUI(content);

                // 【优化】：滚动到底部，显示最新发送的消息
                var scrollContainer = $(".message-scroll-container");
                scrollContainer.scrollTop(scrollContainer[0].scrollHeight);

            } else {
                $("#hintBody").text(data.msg);
            }

            $("#hintModal").modal("show");
            setTimeout(function(){
                $("#hintModal").modal("hide");
                // 移除 location.reload()，避免页面刷新重置懒加载状态
            }, 2000);
        }
    );
}

// 复用之前的消息追加函数，与懒加载的消息样式保持一致
function appendNewMessageToUI(content) {
    var messageUl = $("#messageList");
    var nowTime = new Date().toLocaleString();
    var currentUsername = $("#currentUsername").val(); // 从隐藏域获取当前用户名
    var currentHeaderUrl = $("#currentHeaderUrl").val(); // 从隐藏域获取当前用户头像
    var msgId = new Date().getTime(); // 临时ID，后端返回真实ID后可替换

    // 组装新消息HTML（与懒加载的消息结构、样式完全一致）
    var newHtml = '<li class="media pb-3 pt-3 mb-2" data-msg-id="' + msgId + '">' +
        '<div class="toast show d-lg-block toast-right" role="alert" aria-live="assertive" aria-atomic="true">' +
        '<div class="toast-header">' +
        '<strong class="mr-auto">' + currentUsername + '</strong>' +
        '<small>' + nowTime + '</small>' +
        '<button type="button" class="ml-2 mb-1 close" data-dismiss="toast" aria-label="Close">' +
        '<span aria-hidden="true">&times;</span>' +
        '</button>' +
        '</div>' +
        '<div class="toast-body">' + content + '</div>' +
        '</div>' +
        '<a href="profile.html"><img src="' + currentHeaderUrl + '" class="ml-4 rounded-circle user-header" alt="用户头像" ></a>' +
        '</li>';

    // 追加到消息列表底部（不影响懒加载的顶部历史消息）
    messageUl.append(newHtml);
}