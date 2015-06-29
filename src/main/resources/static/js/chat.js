Chat = (function (window)
{
   // Who needs jQuery??
   var $ = function (e) {
      return document.getElementById(e);
   };

   var doc = document;
   var theHost = location.host;
   var contextPath = location.pathname.slice(0, location.pathname.lastIndexOf("/"));
   var wsScheme = location.protocol == "https:" ? "wss:" : "ws:";
   var connected = false;
   var socket = null;
   var keepAliveTimer = null;
   var msgCounter = 0;
   var state = {0: "CONNECTING", 1: "OPEN", 2: "CLOSING", 3: "CLOSED"};

   function init()
   {
      $("sendBtn").onclick = sendMsgHandler;
      $("chatInput").onkeypress = sendMsgHandler;
      connect(wsScheme + "//" + theHost + contextPath + "/ws");
   }

   function connect(url)
   {
      if (connected) {
         disconnect(true);
         return;
      }

      try {
         socket = new WebSocket(url);
         socket.onopen = onOpen;
         socket.onmessage = onMessage;
         socket.onerror = onError;
         socket.onclose = onClose;
         console.log("WS: " + state[socket.readyState]);
      } catch (e) {
         console.error(e.toString());
      }
   }

   function disconnect(active, reason)
   {
      console.log("WS: disconnect with reason: " + reason);
      connected = false;

      if(reason == "SESSION_DESTROYED") {
         window.location = "/login?expired";
         return;
      }

      if(reason == "ALREADY_OPEN") {
         window.location = "/error?reason=chat_open";
         return;
      }


      $("chatMsgBox").innerHTML = "";
      $("userListBox").innerHTML = "";
      $("statsOutput").style.color = "#666";
      $("statsOutput").textContent = "\u2022 Offline!";

      clearInterval(keepAliveTimer);

      if (active) {
         socket.close(1000);
      }

      socket = null;
   }

   function sendMsgHandler(evt)
   {
      if (!connected) {
         showInfo("Log in first, Stranger!");
         return;
      }

      if (evt.type == "keypress" && evt.keyCode != 13) {
         return;
      }

      var wsMsg = {};
      var chatMsg = {};

      wsMsg.TYPE = "CHAT";
      wsMsg.SUBTYPE = "MSG";
      chatMsg.MSG = $("chatInput").value.trim();
      wsMsg.CHAT_MSG = chatMsg;
      $("chatInput").value = "";

      if (chatMsg.MSG.length > 0) {
         socket.send(JSON.stringify(wsMsg));
      }
   }

   function onOpen(msg)
   {
      console.log("WS: " + state[this.readyState]);
      connected = true;
      var joinMsg = {};
      joinMsg.TYPE = "JOIN";
      socket.send(JSON.stringify(joinMsg));
   }

   function onMessage(message)
   {
      try {
         console.log("WS: " + message.data);
         var wsMsg = JSON.parse(message.data.trim());
         parseMsg(wsMsg);
      } catch (err) {
         console.log("WS: " + err);
      }
   }

   function onClose(msg)
   {
      console.error("WS: onClose()");
      console.log("WS: Disconnected clean: " + msg.wasClean);
      console.log("WS: Code: " + msg.code);
      console.log("WS: " + state[this.readyState]);
      disconnect(false, msg.reason);
   }


   function onError(msg)
   {
      console.error("WS: onError()");
      console.log("WS: Disconnected clean: " + msg.wasClean);
      console.log("WS: Code: " + msg.code);
      console.log("WS: " + state[this.readyState]);
      disconnect(false, msg.reason);
   }

   function parseMsg(msg)
   {
      if(msg.TYPE == "LOGOUT") {
         console.log("WS: Got LOGOUT from server");
         return;
      }

      if (msg.TYPE == "JOIN") {
         $("statsOutput").style.display = "inline-block";
         $("statsOutput").style.color = "#0f0";
         $("statsOutput").textContent = "\u2022 " + msg.STATS_MSG;

         if (msg.USER_LIST) {
            showUserList(msg.USER_LIST);
         }

         // Works only the first time dude!
         if ($("doLoginStranger")) {
            $("doLoginStranger").style.display = "none";
         }

         keepAliveTimer = setInterval(function ()
         {
            var wsMsg = {};
            wsMsg.TYPE = "PING";
            socket.send(JSON.stringify(wsMsg));
         }, 49 * 1000);

         showLoginInfo("Join successful!", "#2f2");

         return;
      }

      if (msg.TYPE == "CHAT") {
         var newMsgElem = doc.createElement("DIV");
         newMsgElem.className = "ChatMsg";
         newMsgElem.id = "msg" + msgCounter;
         newMsgElem.style.color = msg.CHAT_MSG.COLOR;
         newMsgElem.innerHTML = "<div>" + msg.CHAT_MSG.FROM + " ></div><div>" + msg.CHAT_MSG.MSG + "</div>";
         $("chatMsgBox").appendChild(newMsgElem);
         $("chatMsgBox").scrollTop = $("chatOutput").scrollHeight;
         $("msg" + msgCounter).style.left = "0";
         msgCounter++;
         return;
      }

      if (msg.TYPE == "INFO") {
         if (msg.SUBTYPE == "JOIN") {
            showInfo(msg.INFO_MSG, "#99f");
            $("statsOutput").textContent = "\u2022 " + msg.STATS_MSG;
            if (msg.USER_LIST) {
               showUserList(msg.USER_LIST);
            }
         }
      }

   }

   function showInfo(infoMsg)
   {
      $("infoOutput").textContent = infoMsg;
      $("infoOutput").style.top = "8px";

      setTimeout(function ()
      {
         $("infoOutput").style.top = "40px";
      }, 4000);
   }

   function showLoginInfo(infoMsg, color)
   {
      $("loginInfoOutput").textContent = infoMsg;
      $("loginInfoOutput").style.color = color;
      $("loginInfoOutput").style.top = "45px";

      setTimeout(function ()
      {
         $("loginInfoOutput").style.top = "0px";
      }, 3000);
   }


   function showUserList(userList)
   {
      $("userListBox").innerHTML = "";

      for (var i = 0; i < userList.length; i++) {
         var user = userList[i].split("*", 2)[1];
         var color = userList[i].split("*", 2)[0];
         console.log("User:" + user + " Color:" + color);

         var newUserListElem = doc.createElement("DIV");
         newUserListElem.textContent = user;
         newUserListElem.style.color = color;

         $("userListBox").appendChild(newUserListElem);
      }
   }

   init();


})(this);
