window.alert("你知道的陈锡鸿这小子特别狂")
// 定义三个变量
var arg1,arg2,arg3;
// 定义一个整型（Integer）变量
var arg4=5;
// 定义一个浮点型（Float）变量
var arg5=10.0;
// 定义字符型（String）变量
var arg6="你好！";
// 定义一个布尔类型（Boolean）变量
var arg7=true;
// 定义字符串数组//
var arg8=new Array("王","李","赵","张");

//函数定义//
function sayHello(){
    alert("欢迎您来到锡鸿网")
}
 sayHello();
//带参数的函数//
function showName(name){
    console.log("name"+name);
}
//传实参调用//
showName("阿鸿")
//windows对象
// window.confirm("你是否喜欢阿鸿")

// confirm弹出确认框，点击确定返回true，取消返回false
var result = window.confirm("你是否喜欢阿鸿？喜欢奖励看视频看");
timer = window.setTimeout("confirm()",1000)

if(result === true){
    //点确定 → 打开hi.html
    window.open("https://www.bilibili.com/","_blank");
}else{
    //点取消 → 打开百度
    //开局的另外一个选着  window.open("此处可填网站","_blank");
}
//91网https://91porny.com
