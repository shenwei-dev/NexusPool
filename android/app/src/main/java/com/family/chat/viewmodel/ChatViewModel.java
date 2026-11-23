package com.family.chat.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.family.chat.ChatApplication;
import com.family.chat.db.AppDatabase;
import com.family.chat.db.LocalMessage;
import com.family.chat.db.MessageDao;
import com.family.chat.model.ChatMessage;
import com.family.chat.constant.MessageType;
import com.family.chat.protobuf.MessageProto;
import com.family.chat.repository.ChatRepository;
import com.family.chat.util.AvatarManager;
import com.family.chat.util.SessionManager;

import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ChatViewModel extends ViewModel {

    private static final String TAG = "ChatViewModel";

    /** 数据访问层对象 */
    private final ChatRepository chatRepository;

    /** 本地数据库-数据访问层对象 */
    private final MessageDao messageDao;

    /** 当前好友ID */
    private Long currentFriendId;

    private final MutableLiveData<Long> ackEvent = new MutableLiveData<>();

    public LiveData<Long> getAckEvent() {
        return ackEvent;
    }

    /** 存放聊天消息 */
    private LiveData<List<ChatMessage>> messageList;

    /** 存放异常信息 */
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    /** 空参构造初始化 */
    public ChatViewModel() {
        this.chatRepository = new ChatRepository();
        this.messageDao = AppDatabase.getInstance(ChatApplication.getContext()).messageDao();
    }

    public LiveData<List<ChatMessage>> getMessageList(Long friendId) {
        if (messageList == null) {
            LiveData<List<LocalMessage>> localDbLiveData = messageDao.getChatHistory(friendId);
            messageList = Transformations.map(localDbLiveData, localList -> {
                List<ChatMessage> uiList = new ArrayList<>();
                if (localList != null) {
                    for (LocalMessage localMsg : localList) {
                        uiList.add(localMsg.toChatMessage()); // 自动转换
                    }
                }
                return uiList;
            });
        }
        return messageList;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * 初始化聊天上下文，启动连接，注册接收监听
     */
    public void initChat(Long friendId) {
        this.currentFriendId = friendId;
        registerReceiver();
        loadLocalAndSyncCloud();
    }

    /**
     * 发送消息: 先写入本地数据库, 再走 WebSocket 上行
     */
    public void sendTextMessage(String content) {
        if (currentFriendId == null) return;
        chatRepository.sendTextMessage(currentFriendId, content);
    }

    private void registerReceiver() {
        chatRepository.registerMessageReceiver(jsonText -> {
            try {
                JSONObject json = new JSONObject(jsonText);
                if (json.has("status") && json.has("tempId")) {
                    Long tempId = json.getLong("tempId");
                    String status = json.getString("status");
                    if ("success".equals(status)) {
                        long sequence = json.optLong("sequence", 0L);
                        long realMsgId = json.optLong("messageId", 0L);
                        if (realMsgId != 0L) {
                            messageDao.updateRealMsgIdAndStatus(tempId, realMsgId, sequence, ChatMessage.STATUS_SUCCESS);
                            ackEvent.postValue(tempId);
                        }
                    } else {
                        messageDao.updateStatus(tempId, ChatMessage.STATUS_FAILED);
                    }
                } else if (json.optLong("messageId", 0L) > 0 && json.has("senderId") && json.has("content")) {
                    // 仅入库真正的聊天消息帧 (服务端雪花ID恒大于0),
                    // 过滤好友红点/新会话/RTC信令等 messageId=0 的系统帧
                    long senderId = json.getLong("senderId");
                    // 缓存发送者头像 URL, 供聊天界面按 userId 读取
                    AvatarManager.put(senderId, json.optString("senderAvatarUrl"));
                    long messageId = json.optLong("messageId");
                    String content = json.getString("content");
                    int msgType = json.optInt("type", 0);
                    long timestamp = json.optLong("timestamp", System.currentTimeMillis());
                    long sequence = json.optLong("sequence", 0L);
                    LocalMessage newIncomingMsg = new LocalMessage(
                            messageId,
                            senderId,           // 消息发送者 (对方的 friendId)
                            content,            // 消息内容 (文本 / 图片URL / 定位JSON)
                            timestamp,          // 时间戳
                            false,              // isMe = false (靠左显示的对方消息气泡)
                            ChatMessage.STATUS_SUCCESS, // 对方消息默认成功
                            sequence,
                            msgType
                    );
                    messageDao.insert(newIncomingMsg);
                }
            } catch (Exception e) {
                Log.e(TAG, "解析接收消息 JSON 异常", e);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // 页面销毁时解除监听, 防止 WebSocket 回调持有 ViewModel 导致内存泄漏
        chatRepository.unregisterMessageReceiver();
    }

    private void appendStreamChunk(Long messageId, String chunk, Long sequence) {
        // 获取消息列表(聊天记录)
        List<ChatMessage> currentList = messageList.getValue();

        // 如果消息列表为空,则new一个
        if (currentList == null) {
            currentList = new ArrayList<>();
        }

        // 1. 查找内存中是否已经存在该条正在打字的消息
        ChatMessage activeMsg = null;
        int activeIndex = -1;
        for (int i = 0; i < currentList.size(); i++) {
            ChatMessage msg = currentList.get(i);
            if (messageId.equals(msg.getTempId())) {
                activeMsg = msg;
                activeIndex = i;
                break;
            }
        }

        List<ChatMessage> newList = new ArrayList<>(currentList);

        if (activeMsg != null) {
            // 情况 A：如果已经存在，直接在内存中追加字符，不写数据库！速度极快！
            activeMsg.setContent(activeMsg.getContent() + chunk);
            newList.set(activeIndex, activeMsg);
        } else {
            // 情况 B：如果是第一个字符，先清理本地数据库和内存中的“思考中...”占位符，再创建新消息
            messageDao.deleteMessage(-1L); // 物理删除数据库占位
            newList.removeIf(msg -> -1L == msg.getTempId()); // 剔除内存占位

            activeMsg = new ChatMessage(
                    messageId, // 接收消息不需要临时ID
                    currentFriendId,
                    chunk, // 第一个字
                    System.currentTimeMillis(),
                    ChatMessage.STATUS_SUCCESS,
                    false,
                    MessageType.TEXT
            );
            newList.add(activeMsg);
        }

        // 更新聊天记录
//        messageList.postValue(newList);

        LocalMessage localMessage = new LocalMessage(
                activeMsg.getTempId(),
                activeMsg.getReceiverId(),
                activeMsg.getContent(),
                activeMsg.getTimestamp(),
                activeMsg.isMe(),
                activeMsg.getSendStatus(),
                sequence,
                MessageType.TEXT
        );
        messageDao.insert(localMessage);
    }

    private long calculateLastContinuousSeq(List<LocalMessage> recentMessageList) {
        if (recentMessageList == null || recentMessageList.isEmpty()) {
            return 0L;
        }
        for (int i = recentMessageList.size() - 1; i > 0; i--) {
            long currentSeq = recentMessageList.get(i).getSequence();
            long nextSeq = recentMessageList.get(i - 1).getSequence();
            if (nextSeq != currentSeq + 1) {
                Log.d(TAG, "【空洞检测】本地数据在序号 [" + currentSeq + "] 和 [" + nextSeq + "] 之间产生断层");
                return currentSeq;
            }
        }
        return recentMessageList.get(0).getSequence();
    }

    private void loadLocalAndSyncCloud() {
        List<LocalMessage> recentMessageList = messageDao.getRecentMessagesForGapCheck(currentFriendId);
        long watermarkSeq = calculateLastContinuousSeq(recentMessageList);
        Log.d(TAG, "【高水位线计算成功】当前 Watermark = " + watermarkSeq);
        chatRepository.syncDeltaMessages(currentFriendId, watermarkSeq, new ChatRepository.ChatCallback<MessageProto.QueryHistoryResponse>() {
            @Override
            public void onSuccess(MessageProto.QueryHistoryResponse response) {
                isLoading.postValue(false);
                if (response != null && response.getCode() == 1000 && response.getDataList() != null) {
                    List<MessageProto.MsgFrame> serverList = response.getDataList();
                    if (serverList.isEmpty()) return;
                    Long myUserId = SessionManager.getUserId();
                    for (MessageProto.MsgFrame serverMsg : serverList) {
                        boolean isMe = serverMsg.getSenderId() == myUserId;
                        String finalContent = serverMsg.getContent();
                        LocalMessage existingLocalMsg = messageDao.getMessageById(serverMsg.getMessageId());
                        if (existingLocalMsg != null
                                && existingLocalMsg.getContent() != null
                                && existingLocalMsg.getContent().contains("|")) {
                            // 拦截云端覆盖，保护本地图片副本路径
                            finalContent = existingLocalMsg.getContent();
                        }
                        LocalMessage localMessage = new LocalMessage(
                                serverMsg.getMessageId(),
                                currentFriendId,
                                finalContent,
                                serverMsg.getTimestamp(),
                                isMe,
                                ChatMessage.STATUS_SUCCESS,
                                serverMsg.getSequence(),
                                serverMsg.getType()
                        );
                        messageDao.insert(localMessage);
                    }
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "增量同步失败: " + error);
                isLoading.postValue(false);
                errorMessage.postValue("增量同步失败: " + error);
            }
        });
    }

    public void deleteLocalMessage(Long messageId) {
        if (messageId == null) {return;}
        messageDao.deleteMessage(messageId);
    }

    public void sendImageMessage(String localPath) {
        if (currentFriendId == null) return;
        chatRepository.sendImageMessage(localPath, currentFriendId);
    }
}