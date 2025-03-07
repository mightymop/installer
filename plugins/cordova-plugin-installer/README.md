# Android:

### 1. Add plugin
cordova plugin add https://github.com/mightymop/cordova-plugin-installer.git
### 2. For Typescript add following code to main ts file: 
/// &lt;reference types="cordova-plugin-installer" /&gt;<br/>
### 3. Usage:
```
window.androidauto.startActivity(option, success, error);
option = {
  action: string, //required
  callbackurl: string, //required
  componentname: {  //optional
    package: string,
    class: string
  }
  
  //custom params here
}

 window.androidauto.readDataFromContentUri(uri: string, success :((result:any)=>void, error(()=>void);
 //result in base64
```
Example:
```
 window.androidauto.startActivity({action:'target.intent.action',callbackurl:'callback.filter.intent.of.caller.app',componentname:{"package":"target.apps.package.name","class":"target.apps.class.name.fqn"}},
                            function (res:any){
                                console.log(res);
                            },
                            function (err:any){
                                console.error(err);
                            });
```
