# SmartAppZoo

The **SmartAppZoo** dataset contains a total of 3,526 SmartThings apps, including 184 SmartThings official apps, 468 IoTBench third-party apps, and 2,874 Github third-party apps. We always welcome contributions to the dataset from the community.


## Detail

1. We crawled SmartApps from Github repositories between January 1, 2013 and December 31, 2022. 

2. We obtained 184 official apps from the  [**SmartThings Public GitHub Repo**](https://github.com/SmartThingsCommunity/SmartThingsPublic).

3. We obtained 468 cleaned third-party apps from the [**IoTBench dataset**](https://github.com/IoTBench/IoTBench-test-suite), excluding hand-crafted malicious SmartApps.

4. We obtained 2,874 cleaned third-party apps from other Github repositories, avoiding those used for adversarial SmartApp research.

## Conversion

1. We developed a converter script [**ConvertJS.ipynb**](ConvertJS.ipynb) that converts Groovy SmartApps into Node.js SmartApps.

2. The converter accurately converts the fixed format of definition blocks, preference blocks, subscription functions, schedule functions, and device commands from Groovy to Node.js. However, the conversion of other user-defined functions requires the help of the research community. 

3. Currently, the converter can accurately translate 50 Groovy SmartApps into Node.js SmartApps.

## Warning

When visiting the [**Github Third-Party**](Github&#32;Third-Party) folder via a browser, you may receive the following warning:

```
Sorry, we had to truncate this directory to 1,000 files. 1,874 entries were omitted from the list.
```

This is a common practice on Github. Github limits the number of files that can be listed in the webview to 1000. The warning only appears when using a browser, but you can still fork, clone, or download the repository without issues.

## Contributing

We welcome submissions of issues and pull requests. 

## Credit

SmartAppZoo uses SmartApps from the following repositories (in alphabetical order):


[16307vancouver/SmartThingsPublic](https://github.com/16307vancouver/SmartThingsPublic)

[4thjuly/Smartthings-IoT](https://github.com/4thjuly/Smartthings-IoT)

[625alex/SmartThings](https://github.com/625alex/SmartThings)

[67tallchris/SmartThingsPrivate](https://github.com/67tallchris/SmartThingsPrivate)

[702ron/SmartThingsPublic](https://github.com/702ron/SmartThingsPublic)

[7100SW/SmartThingsPublic](https://github.com/7100SW/SmartThingsPublic)

[AYapejian/st-smartdash](https://github.com/AYapejian/st-smartdash)

[AdamJacobMuller/SmartThingsPublic](https://github.com/AdamJacobMuller/SmartThingsPublic)

[Aelfot/SmartThingsCommunity](https://github.com/Aelfot/SmartThingsCommunity)

[AhmedBekhit/SmartThingsPublic](https://github.com/AhmedBekhit/SmartThingsPublic)

[Andreeh/SmartThings](https://github.com/Andreeh/SmartThings)

[AndrewReitz/SmartApps](https://github.com/AndrewReitz/SmartApps)

[AndyRawson/SmartThings](https://github.com/AndyRawson/SmartThings)

[AndyRawson/SmartThingsCode](https://github.com/AndyRawson/SmartThingsCode)

[AndyRawson/SmartThingsTest](https://github.com/AndyRawson/SmartThingsTest)

[Anghus/SmartThings](https://github.com/Anghus/SmartThings)

[AntoineGuilbaud/MySmartThings](https://github.com/AntoineGuilbaud/MySmartThings)

[AtlasKY/Smartthings_DataConsistency_Tool](https://github.com/AtlasKY/Smartthings_DataConsistency_Tool)

[Ayechaw/SmartThings](https://github.com/Ayechaw/SmartThings)

[BamaRayne/EchoSistantApps](https://github.com/BamaRayne/EchoSistantApps)

[BamaRayne/SmartSuite](https://github.com/BamaRayne/SmartSuite)

[BartschLabs/redloro-SmartThings](https://github.com/BartschLabs/redloro-SmartThings)

[Bennezze/SmartThingsPublic](https://github.com/Bennezze/SmartThingsPublic)

[BigWebstas/BW-SmartThings](https://github.com/BigWebstas/BW-SmartThings)

[Bio-RoboticsUNAM/SmartThing](https://github.com/Bio-RoboticsUNAM/SmartThing)

[Bio-RoboticsUNAM/SmartThingsPublic-master-](https://github.com/Bio-RoboticsUNAM/SmartThingsPublic-master-)

[BlacKCaT27/SmartThings-Really-Smart-Mailbox](https://github.com/BlacKCaT27/SmartThings-Really-Smart-Mailbox)

[BluCola/SmartthingsCustomRepo](https://github.com/BluCola/SmartthingsCustomRepo)

[BobRak/OpenHAB-Smartthings](https://github.com/BobRak/OpenHAB-Smartthings)

[BottlecapDave/Smartthings](https://github.com/BottlecapDave/Smartthings)

[BrettSheleski/SmartThings-SmartApp-OAuth-Helper](https://github.com/BrettSheleski/SmartThings-SmartApp-OAuth-Helper)

[BrianJerolleman/Brian-SmartThings](https://github.com/BrianJerolleman/Brian-SmartThings)

[CACgithub/SmartThingsPublic](https://github.com/CACgithub/SmartThingsPublic)

[CNG/smartthings-monitor](https://github.com/CNG/smartthings-monitor)

[CadConsultants/mqtt-bridge-smartthings](https://github.com/CadConsultants/mqtt-bridge-smartthings)

[CamSoper/SmartThings-CamSoper](https://github.com/CamSoper/SmartThings-CamSoper)

[CapnEL/SmartThingsPublic](https://github.com/CapnEL/SmartThingsPublic)

[Carmine7/SmartThingsPublic-](https://github.com/Carmine7/SmartThingsPublic-)

[Cealtea/SmartThings](https://github.com/Cealtea/SmartThings)

[ChainReaction31/OSH-SmartApp](https://github.com/ChainReaction31/OSH-SmartApp)

[Chubaca01/SmartThingsRepo](https://github.com/Chubaca01/SmartThingsRepo)

[ClassicTim1/SleepNumberManager](https://github.com/ClassicTim1/SleepNumberManager)

[ClaudioConsolmagno/samsung-smartthings](https://github.com/ClaudioConsolmagno/samsung-smartthings)

[CoolAutomation/smartthings](https://github.com/CoolAutomation/smartthings)

[CopyCat73/SmartThings-Dev](https://github.com/CopyCat73/SmartThings-Dev)

[Correl8/smartthings](https://github.com/Correl8/smartthings)

[CosmicPuppy/SmartThings](https://github.com/CosmicPuppy/SmartThings)

[CosmicPuppy/SmartThings-ButtonControllerWithDim](https://github.com/CosmicPuppy/SmartThings-ButtonControllerWithDim)

[Cuixe/smartthings](https://github.com/Cuixe/smartthings)

[CyborgMaster/smart-things](https://github.com/CyborgMaster/smart-things)

[CyrilPeponnet/smartthings](https://github.com/CyrilPeponnet/smartthings)

[DMaverock/MySmartThingsProject](https://github.com/DMaverock/MySmartThingsProject)

[DanielOgorchock/ST_Anything](https://github.com/DanielOgorchock/ST_Anything)

[Dannyzen/hubitat_shabbat](https://github.com/Dannyzen/hubitat_shabbat)

[DarcRanger/SmartThingsPublic](https://github.com/DarcRanger/SmartThingsPublic)

[DarrenD05/SmartThingsPublic](https://github.com/DarrenD05/SmartThingsPublic)

[DarwinsDen/Demand-Manager](https://github.com/DarwinsDen/Demand-Manager)

[DarwinsDen/Tesla-Powerwall-Manager](https://github.com/DarwinsDen/Tesla-Powerwall-Manager)

[DaveGut/DEPRECATED-SmartThings_Samsung-WiFi-Audio-Unofficial](https://github.com/DaveGut/DEPRECATED-SmartThings_Samsung-WiFi-Audio-Unofficial)

[DaveGut/DEPRECATED-TP-Link-SmartThings](https://github.com/DaveGut/DEPRECATED-TP-Link-SmartThings)

[DaveGut/bleBox-SmartThings](https://github.com/DaveGut/bleBox-SmartThings)

[Dianoga/my-smartthings](https://github.com/Dianoga/my-smartthings)

[DjAnu/smartthings-1](https://github.com/DjAnu/smartthings-1)

[Dopazo025/SmartThings-with-MyQ](https://github.com/Dopazo025/SmartThings-with-MyQ)

[Draramis/SmartThings](https://github.com/Draramis/SmartThings)

[Dustinmj/SmartThings-RenoTTS](https://github.com/Dustinmj/SmartThings-RenoTTS)

[EricJilot/SmartThings.BatteryMonitor](https://github.com/EricJilot/SmartThings.BatteryMonitor)

[Etrnls/smartthings](https://github.com/Etrnls/smartthings)

[FireAcidAP/SmartThingsPublic](https://github.com/FireAcidAP/SmartThingsPublic)

[FirePanther/Control-Smartthings-Samsung-TVs](https://github.com/FirePanther/Control-Smartthings-Samsung-TVs)

[FireyProtons/FP_SmartThings](https://github.com/FireyProtons/FP_SmartThings)

[FlorianZ/SmartApps](https://github.com/FlorianZ/SmartApps)

[FlorianZ/hadashboard](https://github.com/FlorianZ/hadashboard)

[GarSys/mySmartThings](https://github.com/GarSys/mySmartThings)

[Gavcol7/SmartThings](https://github.com/Gavcol7/SmartThings)

[Git-Chuck/smartthings-hunterdouglasplatinum](https://github.com/Git-Chuck/smartthings-hunterdouglasplatinum)

[Goruti/SmartThings](https://github.com/Goruti/SmartThings)

[Gremlyn/SmartApps](https://github.com/Gremlyn/SmartApps)

[HBOdR/SmartThings](https://github.com/HBOdR/SmartThings)

[Inoshade/Smartthings_DTH-code](https://github.com/Inoshade/Smartthings_DTH-code)

[InovelliUSA/SmartThingsInovelli](https://github.com/InovelliUSA/SmartThingsInovelli)

[Intellithings/SmartThings](https://github.com/Intellithings/SmartThings)

[IoTBench/IoTBench-test-suite](https://github.com/IoTBench/IoTBench-test-suite)

[JAC2703/smart-octopus](https://github.com/JAC2703/smart-octopus)

[JNeubauer3/MySmartThings_dev](https://github.com/JNeubauer3/MySmartThings_dev)

[JZ-SmartThings/SmartThings](https://github.com/JZ-SmartThings/SmartThings)

[JanusNetworks/SmartThingsPublic](https://github.com/JanusNetworks/SmartThingsPublic)

[JasonBSteele/SmartThings](https://github.com/JasonBSteele/SmartThings)

[JayUK/SmartThings-ControlSwitchViaURL](https://github.com/JayUK/SmartThings-ControlSwitchViaURL)

[JayUK/SmartThings-VirtualThermostat](https://github.com/JayUK/SmartThings-VirtualThermostat)

[Jed-Giblin/SmartThingsPublic](https://github.com/Jed-Giblin/SmartThingsPublic)

[Jeffm777/Smartthings-Stuff](https://github.com/Jeffm777/Smartthings-Stuff)

[JoeCraddock/SmartApps](https://github.com/JoeCraddock/SmartApps)

[JoeCraddock/SmartThings-Simple-Rules-Engine](https://github.com/JoeCraddock/SmartThings-Simple-Rules-Engine)

[JohnRucker/CoopBoss-H3Vx](https://github.com/JohnRucker/CoopBoss-H3Vx)

[Johnalius/SmartThings](https://github.com/Johnalius/SmartThings)

[JohntGB/SmartThingZZZPublic](https://github.com/JohntGB/SmartThingZZZPublic)

[JohntGB/SmartThingsZZZ](https://github.com/JohntGB/SmartThingsZZZ)

[Justar818/Smartthings-](https://github.com/Justar818/Smartthings-)

[JustinNale/JustinNale-Smartthings](https://github.com/JustinNale/JustinNale-Smartthings)

[KHouse75/SmartThings-IDE](https://github.com/KHouse75/SmartThings-IDE)

[KenCote/SmartThingsScripts](https://github.com/KenCote/SmartThingsScripts)

[KevinGlinski/SmartthingsScripts](https://github.com/KevinGlinski/SmartthingsScripts)

[Konnichy/SmartThings-Groovy](https://github.com/Konnichy/SmartThings-Groovy)

[KoraHome/KoraSmartThings](https://github.com/KoraHome/KoraSmartThings)

[KristopherKubicki/device-foobot](https://github.com/KristopherKubicki/device-foobot)

[KristopherKubicki/device-type.zwave-motion](https://github.com/KristopherKubicki/device-type.zwave-motion)

[KristopherKubicki/device-yamaha-rx](https://github.com/KristopherKubicki/device-yamaha-rx)

[KristopherKubicki/plantlink-smartapps](https://github.com/KristopherKubicki/plantlink-smartapps)

[KristopherKubicki/smartapp-beep](https://github.com/KristopherKubicki/smartapp-beep)

[KristopherKubicki/smartapp-close-the-garage](https://github.com/KristopherKubicki/smartapp-close-the-garage)

[KristopherKubicki/smartapp-color-temperature](https://github.com/KristopherKubicki/smartapp-color-temperature)

[KristopherKubicki/smartapp-flicker](https://github.com/KristopherKubicki/smartapp-flicker)

[KristopherKubicki/smartapp-http-presence](https://github.com/KristopherKubicki/smartapp-http-presence)

[KristopherKubicki/smartapp-input-select](https://github.com/KristopherKubicki/smartapp-input-select)

[KristopherKubicki/smartapp-light-helper](https://github.com/KristopherKubicki/smartapp-light-helper)

[KristopherKubicki/smartapp-nightlight](https://github.com/KristopherKubicki/smartapp-nightlight)

[KristopherKubicki/smartapp-osram-reset](https://github.com/KristopherKubicki/smartapp-osram-reset)

[KristopherKubicki/smartapp-turn-off-with-motion](https://github.com/KristopherKubicki/smartapp-turn-off-with-motion)

[KristopherKubicki/smartapp-wasp-in-a-box](https://github.com/KristopherKubicki/smartapp-wasp-in-a-box)

[KurtSanders/MySmartThingsPersonal](https://github.com/KurtSanders/MySmartThingsPersonal)

[La0xiu/SmartThings_Cloud-Based_TP-Link-Plugs-Switches-Bulbs](https://github.com/La0xiu/SmartThings_Cloud-Based_TP-Link-Plugs-Switches-Bulbs)

[Ledridge/SmartThings](https://github.com/Ledridge/SmartThings)

[LeeC77/SmartThingsPublic](https://github.com/LeeC77/SmartThingsPublic)

[Lentzbot/Crestron-SmartThings](https://github.com/Lentzbot/Crestron-SmartThings)

[LimeNinja/smartthings](https://github.com/LimeNinja/smartthings)

[LiveMike78/SmartThings](https://github.com/LiveMike78/SmartThings)

[LynxTechTeam/STSmartApp-Handler](https://github.com/LynxTechTeam/STSmartApp-Handler)

[MMaschin/DSC-IT100-Alarm](https://github.com/MMaschin/DSC-IT100-Alarm)

[MRobi1/smartthings](https://github.com/MRobi1/smartthings)

[MadMouse/SmartThings](https://github.com/MadMouse/SmartThings)

[ManuRiver/DSC-alarm-for-Hubitat-using-nodejs-thru-serial-TI-100](https://github.com/ManuRiver/DSC-alarm-for-Hubitat-using-nodejs-thru-serial-TI-100)

[MarianMitschke/Smartthings-Fritz-DECT-200-Controller](https://github.com/MarianMitschke/Smartthings-Fritz-DECT-200-Controller)

[Mariano-Github/Simple-Event-Logger-SmartApp](https://github.com/Mariano-Github/Simple-Event-Logger-SmartApp)

[Mariano-Github/Smartthings-smartapp](https://github.com/Mariano-Github/Smartthings-smartapp)

[Mark52r/smartthings_hub](https://github.com/Mark52r/smartthings_hub)

[MatthewMcD/SmartThings](https://github.com/MatthewMcD/SmartThings)

[Mavrrick/ArloAssistant](https://github.com/Mavrrick/ArloAssistant)

[Mavrrick/Smartthings-by-Mavrrick](https://github.com/Mavrrick/Smartthings-by-Mavrrick)

[Mdleal/smartthings](https://github.com/Mdleal/smartthings)

[Mellit7/Smartthings-SqueezeBox-Control](https://github.com/Mellit7/Smartthings-SqueezeBox-Control)

[MengshanChen/SmartThings](https://github.com/MengshanChen/SmartThings)

[MikeJoyce/door-monitor](https://github.com/MikeJoyce/door-monitor)

[Mikebru10/SmartThingsPublic](https://github.com/Mikebru10/SmartThingsPublic)

[MonicaPintoAlarcon/cursoIoTApplications](https://github.com/MonicaPintoAlarcon/cursoIoTApplications)

[MrMatt57/SmartThings](https://github.com/MrMatt57/SmartThings)

[Mrenerd/Simplisafe-Smartthings](https://github.com/Mrenerd/Simplisafe-Smartthings)

[Ninja1000/SmartThings_MyQ](https://github.com/Ninja1000/SmartThings_MyQ)

[Nomad-Tech/DaSmartThings](https://github.com/Nomad-Tech/DaSmartThings)

[NonLogicalDev/lib.SmartthingsExtensions](https://github.com/NonLogicalDev/lib.SmartthingsExtensions)

[NotQuiteAdam/SmartThings](https://github.com/NotQuiteAdam/SmartThings)

[Nuukem/SmartThings](https://github.com/Nuukem/SmartThings)

[Obighbyd/SmartThings](https://github.com/Obighbyd/SmartThings)

[Oendaril/TotalConnectAsync](https://github.com/Oendaril/TotalConnectAsync)

[Oliver-66/SmartThingsPublic](https://github.com/Oliver-66/SmartThingsPublic)

[OmenWild/SmartThings-RandomHue](https://github.com/OmenWild/SmartThings-RandomHue)

[PJayB/SmartThingsApp](https://github.com/PJayB/SmartThingsApp)

[Pacman45/My-Smartthings-Apps](https://github.com/Pacman45/My-Smartthings-Apps)

[PeterLarsen-CPH/HallwayLight-for-SmartThings](https://github.com/PeterLarsen-CPH/HallwayLight-for-SmartThings)

[Petezah/SmartThings](https://github.com/Petezah/SmartThings)

[PhilDye/loop-energy](https://github.com/PhilDye/loop-energy)

[PockyBum522/smartthings_LAN_get_requester_button_controller](https://github.com/PockyBum522/smartthings_LAN_get_requester_button_controller)

[Potturusoft/MySmartThings](https://github.com/Potturusoft/MySmartThings)

[PurelyNicole/SmartThingsApps](https://github.com/PurelyNicole/SmartThingsApps)

[RMoRobert/SmartThings](https://github.com/RMoRobert/SmartThings)

[RandallOfLegend/SmartThings](https://github.com/RandallOfLegend/SmartThings)

[RedSerenity/SmartThings](https://github.com/RedSerenity/SmartThings)

[Roadkill43/SmartThings-roadkill](https://github.com/Roadkill43/SmartThings-roadkill)

[RobinShipston/Tesla-Powerwall](https://github.com/RobinShipston/Tesla-Powerwall)

[RodrigoExperthome/SmartThingsExpertHome](https://github.com/RodrigoExperthome/SmartThingsExpertHome)

[RogerSelwyn/SmartThings](https://github.com/RogerSelwyn/SmartThings)

[RonV42/smartthings](https://github.com/RonV42/smartthings)

[Rooks103/MattyP-SmartApps](https://github.com/Rooks103/MattyP-SmartApps)

[RudeDog69/SmartThings_Humidifier_Control](https://github.com/RudeDog69/SmartThings_Humidifier_Control)

[RudiP/SmartThingsRudi](https://github.com/RudiP/SmartThingsRudi)

[Runeoth/SmartThings](https://github.com/Runeoth/SmartThings)

[RuthlessRhino/SmartThingsPublic](https://github.com/RuthlessRhino/SmartThingsPublic)

[SANdood/ActiON-Dashboard](https://github.com/SANdood/ActiON-Dashboard)

[SANdood/Custom-ST-Devices](https://github.com/SANdood/Custom-ST-Devices)

[SANdood/Ecobee](https://github.com/SANdood/Ecobee)

[SANdood/Ecobee-Suite](https://github.com/SANdood/Ecobee-Suite)

[SANdood/Green-Smart-HW-Recirculator](https://github.com/SANdood/Green-Smart-HW-Recirculator)

[SANdood/LEDStatusManager](https://github.com/SANdood/LEDStatusManager)

[SANdood/SmartThings-Fixed](https://github.com/SANdood/SmartThings-Fixed)

[Sanfe75/MySmartThingsPublic](https://github.com/Sanfe75/MySmartThingsPublic)

[Sastozki/Smartthings-SmartApps](https://github.com/Sastozki/Smartthings-SmartApps)

[SkyJedi/SmartThings](https://github.com/SkyJedi/SmartThings)

[Slbenficaboy/SmartThings](https://github.com/Slbenficaboy/SmartThings)

[Slowfoxtrot/SmartThings](https://github.com/Slowfoxtrot/SmartThings)

[Slugofest/SmartThingsPublic](https://github.com/Slugofest/SmartThingsPublic)

[SmartThingsCommunity/Code](https://github.com/SmartThingsCommunity/Code)

[SmartThingsCommunity/SmartThingsPublic](https://github.com/SmartThingsCommunity/SmartThingsPublic)

[SmartThingsUle/DLNA-PLAYER](https://github.com/SmartThingsUle/DLNA-PLAYER)

[SmartThingsUle/Echo-Speakers](https://github.com/SmartThingsUle/Echo-Speakers)

[SmartThingsUle/SmartAlarmMOD](https://github.com/SmartThingsUle/SmartAlarmMOD)

[SmartThingsUle/TCP-Connectd](https://github.com/SmartThingsUle/TCP-Connectd)

[SpecialK1417/SmartThingsCustom](https://github.com/SpecialK1417/SmartThingsCustom)

[Spinningbull/iotplayground](https://github.com/Spinningbull/iotplayground)

[StackStorm-Exchange/stackstorm-smartthings](https://github.com/StackStorm-Exchange/stackstorm-smartthings)

[StevanWarwick/Thermostat-Setpoint-Limits](https://github.com/StevanWarwick/Thermostat-Setpoint-Limits)

[SteveTheGeekHA/MagicCubeSmartapp](https://github.com/SteveTheGeekHA/MagicCubeSmartapp)

[StevenJonSmith/SmartThings](https://github.com/StevenJonSmith/SmartThings)

[StevenNCSU/SmartThings_Public](https://github.com/StevenNCSU/SmartThings_Public)

[StrykerSKS/Ecobee](https://github.com/StrykerSKS/Ecobee)

[StrykerSKS/SmartThings](https://github.com/StrykerSKS/SmartThings)

[StrykerSKS/SmartThingsTracker](https://github.com/StrykerSKS/SmartThingsTracker)

[StuRams/Cloud-Based_TP-Link-to-SmartThings-Integration](https://github.com/StuRams/Cloud-Based_TP-Link-to-SmartThings-Integration)

[Tallykool/TP-Link-SmartThings](https://github.com/Tallykool/TP-Link-SmartThings)

[Tallykool/smartthings-dsc-alarm.git2](https://github.com/Tallykool/smartthings-dsc-alarm.git2)

[TehSmartThing/SmartApps](https://github.com/TehSmartThing/SmartApps)

[TekniskSupport/ST-HA-Multi-button-remote](https://github.com/TekniskSupport/ST-HA-Multi-button-remote)

[TekniskSupport/SmartThings](https://github.com/TekniskSupport/SmartThings)

[TheCase/SmartThings](https://github.com/TheCase/SmartThings)

[TheFuzz4/SmartThingsSplunkLogger](https://github.com/TheFuzz4/SmartThingsSplunkLogger)

[TheHomeRemote/HomeRemote.SmartApp](https://github.com/TheHomeRemote/HomeRemote.SmartApp)

[Toliver182/SmartThings-Kodi](https://github.com/Toliver182/SmartThings-Kodi)

[Toliver182/SmartThings-KodiControl-Callback](https://github.com/Toliver182/SmartThings-KodiControl-Callback)

[TomMirf/SmartThings](https://github.com/TomMirf/SmartThings)

[TomerFi/home_assistant_smartthings_bridge](https://github.com/TomerFi/home_assistant_smartthings_bridge)

[TopherSavoie/SmartThings_ULM](https://github.com/TopherSavoie/SmartThings_ULM)

[Tsaaek/Smartthings](https://github.com/Tsaaek/Smartthings)

[Welasco/SmartThingsAqualinkd](https://github.com/Welasco/SmartThingsAqualinkd)

[Welasco/SmartthingsFanControl](https://github.com/Welasco/SmartthingsFanControl)

[Welasco/SmartthingsFanControlHubitat](https://github.com/Welasco/SmartthingsFanControlHubitat)

[Wob76/SmartThings](https://github.com/Wob76/SmartThings)

[WooBooung/BooungThings](https://github.com/WooBooung/BooungThings)

[Wovyn/SmartThings](https://github.com/Wovyn/SmartThings)

[Xtropy74/X-SmartThings](https://github.com/Xtropy74/X-SmartThings)

[YouOdysseyLeif/smartthings-DH](https://github.com/YouOdysseyLeif/smartthings-DH)

[aakashcode/smartthings](https://github.com/aakashcode/smartthings)

[abcvalenti/check-if-its-locked](https://github.com/abcvalenti/check-if-its-locked)

[aberkeme/AHB_SmartThings_SmartApps_AutoLock](https://github.com/aberkeme/AHB_SmartThings_SmartApps_AutoLock)

[abethcrane/ms-band-smartthings-tile](https://github.com/abethcrane/ms-band-smartthings-tile)

[abuttino/SmartThingsCustom](https://github.com/abuttino/SmartThingsCustom)

[ac7ss/SmartThings-Private](https://github.com/ac7ss/SmartThings-Private)

[adamahrens/smart-apps](https://github.com/adamahrens/smart-apps)

[adamclark-dev/smartthings-text-to-speech](https://github.com/adamclark-dev/smartthings-text-to-speech)

[adamkempenich/Simple-Sprinklers](https://github.com/adamkempenich/Simple-Sprinklers)

[adampv/smartthings](https://github.com/adampv/smartthings)

[adamzolotarev/My-Smartthings](https://github.com/adamzolotarev/My-Smartthings)

[adein/smartthings](https://github.com/adein/smartthings)

[aderusha/SmartThings](https://github.com/aderusha/SmartThings)

[adey/bangali](https://github.com/adey/bangali)

[adkinsz/Smartthings](https://github.com/adkinsz/Smartthings)

[aduyng/smart-switch-for-garage](https://github.com/aduyng/smart-switch-for-garage)

[aduyng/smatch-switch-for-toilet](https://github.com/aduyng/smatch-switch-for-toilet)

[aduyng/smatthings-report-sensor-values-to-firebase](https://github.com/aduyng/smatthings-report-sensor-values-to-firebase)

[ady624/HomeCloudHub](https://github.com/ady624/HomeCloudHub)

[agent511/SmartThingsSaved](https://github.com/agent511/SmartThingsSaved)

[ajcarpenter/smartthings-boiler-switch](https://github.com/ajcarpenter/smartthings-boiler-switch)

[ajpri/Ill-be-back-SA](https://github.com/ajpri/Ill-be-back-SA)

[ajpri/UselessSmartApp](https://github.com/ajpri/UselessSmartApp)

[ajvwhite/homebridge-smartthings-routine-triggers](https://github.com/ajvwhite/homebridge-smartthings-routine-triggers)

[akihamalainen/SmartThings](https://github.com/akihamalainen/SmartThings)

[alacercogitatus/smartthings_splunk](https://github.com/alacercogitatus/smartthings_splunk)

[alainmenag/SmartThings](https://github.com/alainmenag/SmartThings)

[alexfu/smartthings](https://github.com/alexfu/smartthings)

[alexgrahamuk/smartthings-slack-reporter](https://github.com/alexgrahamuk/smartthings-slack-reporter)

[alisdairjsmyth/smartthings-ambiclimate](https://github.com/alisdairjsmyth/smartthings-ambiclimate)

[allandin/TeslaChargePortOpener](https://github.com/allandin/TeslaChargePortOpener)

[alttext/SmartThings-1](https://github.com/alttext/SmartThings-1)

[alvaromi/smartapps](https://github.com/alvaromi/smartapps)

[alyc100/SmartThingsBETA](https://github.com/alyc100/SmartThingsBETA)

[andersonwembleyposavatz/SmartThingsPrivate](https://github.com/andersonwembleyposavatz/SmartThingsPrivate)

[anderssv/smartthings-code](https://github.com/anderssv/smartthings-code)

[anderssv/smartthings-thirdparty](https://github.com/anderssv/smartthings-thirdparty)

[anderssv/smartthings-verisure](https://github.com/anderssv/smartthings-verisure)

[andy-sheen/smartthings](https://github.com/andy-sheen/smartthings)

[andyWallhack/SmartThingsPublic](https://github.com/andyWallhack/SmartThingsPublic)

[anmnguyen/smartthings](https://github.com/anmnguyen/smartthings)

[anthonyrhook/SmartThingsApps](https://github.com/anthonyrhook/SmartThingsApps)

[anuragsimgeker/simplethings-ui](https://github.com/anuragsimgeker/simplethings-ui)

[anuragsimgeker/things-ui](https://github.com/anuragsimgeker/things-ui)

[apa-1/smartthings](https://github.com/apa-1/smartthings)

[appzer/smartapp.pushsafer](https://github.com/appzer/smartapp.pushsafer)

[arcreative/homebridge-smartthings-routine](https://github.com/arcreative/homebridge-smartthings-routine)

[arishtjain/smartThings-xooa](https://github.com/arishtjain/smartThings-xooa)

[arishtjain/smartThingsLog](https://github.com/arishtjain/smartThingsLog)

[arnbme/SmartThingsPublic](https://github.com/arnbme/SmartThingsPublic)

[aromka/myqcontroller](https://github.com/aromka/myqcontroller)

[arustandi/homebridge-smartthings](https://github.com/arustandi/homebridge-smartthings)

[arvonth/at-home-dark](https://github.com/arvonth/at-home-dark)

[asayler-st/SmartThingsErocm123](https://github.com/asayler-st/SmartThingsErocm123)

[asayler-st/SmartThingsJbisson](https://github.com/asayler-st/SmartThingsJbisson)

[ashutosh1982/smartthings-8](https://github.com/ashutosh1982/smartthings-8)

[asishrs/smartthings](https://github.com/asishrs/smartthings)

[asishrs/smartthings-ringalarm](https://github.com/asishrs/smartthings-ringalarm)

[astaiyeb/SmartThing](https://github.com/astaiyeb/SmartThing)

[astrowings/SmartThings](https://github.com/astrowings/SmartThings)

[augoisms/smartthings](https://github.com/augoisms/smartthings)

[austinrfnd/SmartThingsSeinfelder](https://github.com/austinrfnd/SmartThingsSeinfelder)

[austinrfnd/StrangerThingsSmartThings](https://github.com/austinrfnd/StrangerThingsSmartThings)

[automaton82/st-precise-thermostat](https://github.com/automaton82/st-precise-thermostat)

[auual128/SmartThingsPublic](https://github.com/auual128/SmartThingsPublic)

[awpalmbach/DidItRun](https://github.com/awpalmbach/DidItRun)

[ayeshakr/Monitor](https://github.com/ayeshakr/Monitor)

[azhurb/smartthings-advanced-virtual-thermostat](https://github.com/azhurb/smartthings-advanced-virtual-thermostat)

[baknayeon/smartvisual](https://github.com/baknayeon/smartvisual)

[baldeagle072/SmartThingsOther](https://github.com/baldeagle072/SmartThingsOther)

[baldeagle072/smartthings-the_one_thermostat](https://github.com/baldeagle072/smartthings-the_one_thermostat)

[baldeagle072/thermostat-setpoint-manager](https://github.com/baldeagle072/thermostat-setpoint-manager)

[barabba9174/SmartThingsPublicOld](https://github.com/barabba9174/SmartThingsPublicOld)

[barabba9174/Smartthings-barabba9174](https://github.com/barabba9174/Smartthings-barabba9174)

[barmst02/MySmartThings](https://github.com/barmst02/MySmartThings)

[batlinal/smartthings](https://github.com/batlinal/smartthings)

[baum13/WIP](https://github.com/baum13/WIP)

[baurandr/smartthings_custom](https://github.com/baurandr/smartthings_custom)

[bburner/SmartThings](https://github.com/bburner/SmartThings)

[bcn-israelforst/if_smartthings](https://github.com/bcn-israelforst/if_smartthings)

[bdahlem/Easy-Button](https://github.com/bdahlem/Easy-Button)

[bdwilson/PlotWatt-SmartThings-Logger](https://github.com/bdwilson/PlotWatt-SmartThings-Logger)

[bdwilson/SEG-Logger](https://github.com/bdwilson/SEG-Logger)

[bdwilson/SmartThings-GeoHopper-Presence](https://github.com/bdwilson/SmartThings-GeoHopper-Presence)

[bdwilson/SmartThings-TotalConnect-Device](https://github.com/bdwilson/SmartThings-TotalConnect-Device)

[bdwilson/ThingSpeak-Energy-Logger](https://github.com/bdwilson/ThingSpeak-Energy-Logger)

[beckyricha/SmartThings-TV-Channels](https://github.com/beckyricha/SmartThings-TV-Channels)

[behm/smartapps](https://github.com/behm/smartapps)

[bendeitch/MySmartThings](https://github.com/bendeitch/MySmartThings)

[bennyalvey/SmartThings](https://github.com/bennyalvey/SmartThings)

[bhroma/First-Smartapp](https://github.com/bhroma/First-Smartapp)

[big5jeff/lock4jeff](https://github.com/big5jeff/lock4jeff)

[bigjuanpa/Work](https://github.com/bigjuanpa/Work)

[bigpunk6/smartapp.pump.freeze.protection](https://github.com/bigpunk6/smartapp.pump.freeze.protection)

[bigworm76/SmartThingsPublic](https://github.com/bigworm76/SmartThingsPublic)

[billbrazeal/bbDeBo](https://github.com/billbrazeal/bbDeBo)

[billnapier/smartthings](https://github.com/billnapier/smartthings)

[billsq/smartthings-cambridge-audio](https://github.com/billsq/smartthings-cambridge-audio)

[billsq/smartthings-denon-avr-2017](https://github.com/billsq/smartthings-denon-avr-2017)

[binhton/SmartThings-Apps](https://github.com/binhton/SmartThings-Apps)

[biocomp/EcoNet](https://github.com/biocomp/EcoNet)

[bkeifer/smartapp.log-to-graphite](https://github.com/bkeifer/smartapp.log-to-graphite)

[bkeifer/smartapp.summon-the-dog](https://github.com/bkeifer/smartapp.summon-the-dog)

[bkeifer/smartthings](https://github.com/bkeifer/smartthings)

[blakebuck/ST-Front-Light-Automation](https://github.com/blakebuck/ST-Front-Light-Automation)

[blaksec/SmartThingsPublic](https://github.com/blaksec/SmartThingsPublic)

[blebson/Smart-Security-Camera](https://github.com/blebson/Smart-Security-Camera)

[blebson/smart-night-vision](https://github.com/blebson/smart-night-vision)

[bloft/MySmartthings](https://github.com/bloft/MySmartthings)

[bmmiller/SmartThings](https://github.com/bmmiller/SmartThings)

[bobblesg/SmartThings-6](https://github.com/bobblesg/SmartThings-6)

[bobruddy/SmartThingsPublic](https://github.com/bobruddy/SmartThingsPublic)

[boczar/SmartThingsPublic](https://github.com/boczar/SmartThingsPublic)

[bogdanripa/Bticino_smartthings](https://github.com/bogdanripa/Bticino_smartthings)

[bogusfocused/ha_lan_smartthings](https://github.com/bogusfocused/ha_lan_smartthings)

[braclark/MySmartThings](https://github.com/braclark/MySmartThings)

[bradmb/smartthings-samsung-tv](https://github.com/bradmb/smartthings-samsung-tv)

[brandonartz/pirelay](https://github.com/brandonartz/pirelay)

[bravenel/Lutron](https://github.com/bravenel/Lutron)

[braytonstafford/google-assistant-relay](https://github.com/braytonstafford/google-assistant-relay)

[brbarret/InsteonIntegrationREST](https://github.com/brbarret/InsteonIntegrationREST)

[brbeaird/SmartThings_SenseMonitor](https://github.com/brbeaird/SmartThings_SenseMonitor)

[brbeaird/SmartThings_WeatherAlarm](https://github.com/brbeaird/SmartThings_WeatherAlarm)

[breity55/SmartThings](https://github.com/breity55/SmartThings)

[brendanSapience/Samsung-Smartthings-Custom-Rest-API](https://github.com/brendanSapience/Samsung-Smartthings-Custom-Rest-API)

[brentmaxwell/SmartThings](https://github.com/brentmaxwell/SmartThings)

[brianmsailing/SmartThingsCode](https://github.com/brianmsailing/SmartThingsCode)

[brlodi/miles](https://github.com/brlodi/miles)

[brockwddb/SmartThings-Mode-By-Day](https://github.com/brockwddb/SmartThings-Mode-By-Day)

[broox/smart-things-apps](https://github.com/broox/smart-things-apps)

[bryanlogan/SmartThings](https://github.com/bryanlogan/SmartThings)

[bsileo/SmartThings_Pentair](https://github.com/bsileo/SmartThings_Pentair)

[bspranger/WaterHeaterControl](https://github.com/bspranger/WaterHeaterControl)

[btforrest/mitchpond-SmartThingsPublic2](https://github.com/btforrest/mitchpond-SmartThingsPublic2)

[budney/home-energy-logger](https://github.com/budney/home-energy-logger)

[buzzkc/SmartThingsPublic](https://github.com/buzzkc/SmartThingsPublic)

[caesarsghost/device-type.condor4monitor](https://github.com/caesarsghost/device-type.condor4monitor)

[caglar10ur/SmartThings](https://github.com/caglar10ur/SmartThings)

[calebp/SmartThings](https://github.com/calebp/SmartThings)

[carldebilly/SmartThings-Stuff](https://github.com/carldebilly/SmartThings-Stuff)

[carnar/smartthings](https://github.com/carnar/smartthings)

[carvoyant/SmartThings](https://github.com/carvoyant/SmartThings)

[caseyhelbling/smartthings-apps](https://github.com/caseyhelbling/smartthings-apps)

[caseymqn/SmartThings](https://github.com/caseymqn/SmartThings)

[casper-gh/smartapp-turn-off-with-motion-with-threshold](https://github.com/casper-gh/smartapp-turn-off-with-motion-with-threshold)

[castlecole/SmartApps](https://github.com/castlecole/SmartApps)

[cblomart/smartthings](https://github.com/cblomart/smartthings)

[cbuk/Samsung-Smart-Things](https://github.com/cbuk/Samsung-Smart-Things)

[ccsturgi/smrtthng-modesettings](https://github.com/ccsturgi/smrtthng-modesettings)

[cdatwood/SmartThings-Total-Connect-Sensor-Update](https://github.com/cdatwood/SmartThings-Total-Connect-Sensor-Update)

[cetheridge30/SmartThings-Repo](https://github.com/cetheridge30/SmartThings-Repo)

[cfinke/SmartApps](https://github.com/cfinke/SmartApps)

[chadly/smartthings](https://github.com/chadly/smartthings)

[chancsc/SmartThings-SC](https://github.com/chancsc/SmartThings-SC)

[changmang2003/homebridge-smartthings](https://github.com/changmang2003/homebridge-smartthings)

[chazmantir/SmartThings-RingFloodlightServer](https://github.com/chazmantir/SmartThings-RingFloodlightServer)

[chhall1982/SmartThings_MyStuff](https://github.com/chhall1982/SmartThings_MyStuff)

[chhjel/Plex2SmartThings](https://github.com/chhjel/Plex2SmartThings)

[chrishalebarnes/smartapps](https://github.com/chrishalebarnes/smartapps)

[chriskooken/SmartThings](https://github.com/chriskooken/SmartThings)

[christianmadden/smartthings-autophrases](https://github.com/christianmadden/smartthings-autophrases)

[christianmadden/smartthings-home-api](https://github.com/christianmadden/smartthings-home-api)

[chuckster53/SmartThings](https://github.com/chuckster53/SmartThings)

[cipherforge/smartthings](https://github.com/cipherforge/smartthings)

[ckairinc/SmartThingsLocalCK](https://github.com/ckairinc/SmartThingsLocalCK)

[cl0udninja/raspberrypi.smartthings](https://github.com/cl0udninja/raspberrypi.smartthings)

[clang13/hunter-douglas-powerview](https://github.com/clang13/hunter-douglas-powerview)

[clipman/Smartthings](https://github.com/clipman/Smartthings)

[coolcatiger/smartthings_sonos_door_left_open](https://github.com/coolcatiger/smartthings_sonos_door_left_open)

[coolkev/smartthings-alarm](https://github.com/coolkev/smartthings-alarm)

[copy-ninja/SmartThings_KabutoAlarmPanel](https://github.com/copy-ninja/SmartThings_KabutoAlarmPanel)

[copy-ninja/SmartThings_RainMachine](https://github.com/copy-ninja/SmartThings_RainMachine)

[copy-ninja/SmartThings_RheemEcoNet](https://github.com/copy-ninja/SmartThings_RheemEcoNet)

[copy-ninja/SmartThings_iComfort](https://github.com/copy-ninja/SmartThings_iComfort)

[coreyrjackson/SmartThings](https://github.com/coreyrjackson/SmartThings)

[cosborn89/SmartThings](https://github.com/cosborn89/SmartThings)

[cosmicc/SmartThings-Galaxy-Home](https://github.com/cosmicc/SmartThings-Galaxy-Home)

[courtney-rosenthal/SmartThings](https://github.com/courtney-rosenthal/SmartThings)

[cramforce/setTimeout](https://github.com/cramforce/setTimeout)

[crazyjncsu/smartthings](https://github.com/crazyjncsu/smartthings)

[crazystick/SmartThings](https://github.com/crazystick/SmartThings)

[crepric/SmartThingsCubeSwitch](https://github.com/crepric/SmartThingsCubeSwitch)

[crockerg/SmartThings](https://github.com/crockerg/SmartThings)

[crousky/night-alarm](https://github.com/crousky/night-alarm)

[cschone/SmartThings](https://github.com/cschone/SmartThings)

[csdozier/device-concord4](https://github.com/csdozier/device-concord4)

[csdozier/device-nexia-thermostat](https://github.com/csdozier/device-nexia-thermostat)

[csdozier/device-pioneer-vsx](https://github.com/csdozier/device-pioneer-vsx)

[cswales/wireless-sensor-decode](https://github.com/cswales/wireless-sensor-decode)

[cumpstey/Cwm.SmartThings](https://github.com/cumpstey/Cwm.SmartThings)

[curtisc19/SmartThingsCoffeeGroovy](https://github.com/curtisc19/SmartThingsCoffeeGroovy)

[cvpcs/smartthings](https://github.com/cvpcs/smartthings)

[cyclingengineer/UpnpHomeAutomationBridge](https://github.com/cyclingengineer/UpnpHomeAutomationBridge)

[cznkane/HomeAutomation](https://github.com/cznkane/HomeAutomation)

[d60dvr/DarwinsDen_Smartthings](https://github.com/d60dvr/DarwinsDen_Smartthings)

[d8adrvn/smart_sprinkler](https://github.com/d8adrvn/smart_sprinkler)

[dachnikme/SmartThingsPublic](https://github.com/dachnikme/SmartThingsPublic)

[dadlersan/MyST](https://github.com/dadlersan/MyST)

[dan-lambert/SmartThings](https://github.com/dan-lambert/SmartThings)

[dan06/SmartThings---Vacation-Lights](https://github.com/dan06/SmartThings---Vacation-Lights)

[dan06/SmartThings-Goodnight](https://github.com/dan06/SmartThings-Goodnight)

[dan06/SmartThings-LG-Smart-TV](https://github.com/dan06/SmartThings-LG-Smart-TV)

[dangoscomb/smartapps](https://github.com/dangoscomb/smartapps)

[danielsjf/erocm123_SmartThingsPublic](https://github.com/danielsjf/erocm123_SmartThingsPublic)

[dankraus/seinfeld-door-google-home-cast](https://github.com/dankraus/seinfeld-door-google-home-cast)

[dannyc83/smartthings](https://github.com/dannyc83/smartthings)

[danyulsan91/SmartThings-danyulsan91](https://github.com/danyulsan91/SmartThings-danyulsan91)

[darinspivey/smartthings-apps](https://github.com/darinspivey/smartthings-apps)

[dario-rossi/SmartThings-Personal-Library](https://github.com/dario-rossi/SmartThings-Personal-Library)

[darksun/st-lan-example](https://github.com/darksun/st-lan-example)

[dasalien/SmartThingsPrivate](https://github.com/dasalien/SmartThingsPrivate)

[dashie-app/smartthings](https://github.com/dashie-app/smartthings)

[daveyiv/SmartThings](https://github.com/daveyiv/SmartThings)

[davglass/st-beacon-bacon](https://github.com/davglass/st-beacon-bacon)

[davidbliss/SmartThings_Multi-switch](https://github.com/davidbliss/SmartThings_Multi-switch)

[davidsalvadorpt/SmartThingsOutSystemsConnector](https://github.com/davidsalvadorpt/SmartThingsOutSystemsConnector)

[dcm220/SmartThingsPublic](https://github.com/dcm220/SmartThingsPublic)

[dcyonce/SmartThings-Private](https://github.com/dcyonce/SmartThings-Private)

[ddsmyers/smartthings-installer](https://github.com/ddsmyers/smartthings-installer)

[dedouard/SmartThingsPublic](https://github.com/dedouard/SmartThingsPublic)

[densom/smartthings-elasticlogger](https://github.com/densom/smartthings-elasticlogger)

[desertblade/Beddi_Connect](https://github.com/desertblade/Beddi_Connect)

[devicebuilder/Blue_Iris_Server_for_ST](https://github.com/devicebuilder/Blue_Iris_Server_for_ST)

[dgem2015/SmartThingsPublic](https://github.com/dgem2015/SmartThingsPublic)

[dguindon/mySmartThings](https://github.com/dguindon/mySmartThings)

[dianakoh/STAirPollutionWeather](https://github.com/dianakoh/STAirPollutionWeather)

[digitalBush/smartthings](https://github.com/digitalBush/smartthings)

[dirkhain/smartthings](https://github.com/dirkhain/smartthings)

[dkirker/smartthings-lockitron](https://github.com/dkirker/smartthings-lockitron)

[dkirker/smartthings-sandbox](https://github.com/dkirker/smartthings-sandbox)

[dlaporte/SmartThings](https://github.com/dlaporte/SmartThings)

[dlmayhugh/SmartThings](https://github.com/dlmayhugh/SmartThings)

[dlpwx/SmartThings](https://github.com/dlpwx/SmartThings)

[dmcconnell68/SmartThingsPublic](https://github.com/dmcconnell68/SmartThingsPublic)

[dmongeau/smartthings](https://github.com/dmongeau/smartthings)

[dneilan/OurPlace](https://github.com/dneilan/OurPlace)

[docwisdom/SmartThings](https://github.com/docwisdom/SmartThings)

[dotanshai/SmartThings](https://github.com/dotanshai/SmartThings)

[dougdale/SmartThings](https://github.com/dougdale/SmartThings)

[dr-dave-w/smart-things](https://github.com/dr-dave-w/smart-things)

[dr1rrb/SmartThings-Fitbit](https://github.com/dr1rrb/SmartThings-Fitbit)

[dr1rrb/SmartThings-UniFiClients](https://github.com/dr1rrb/SmartThings-UniFiClients)

[dr1rrb/smartthings2mqtt](https://github.com/dr1rrb/smartthings2mqtt)

[dr1rrb/st-smartthingstostart](https://github.com/dr1rrb/st-smartthingstostart)

[dragokvm/SmartApps](https://github.com/dragokvm/SmartApps)

[drandyhaas/SmartThingsPublic](https://github.com/drandyhaas/SmartThingsPublic)

[drewski11/Is-Home-Secure-Smart-App](https://github.com/drewski11/Is-Home-Secure-Smart-App)

[dsggregory/SmartThings](https://github.com/dsggregory/SmartThings)

[dvanwinkle/smartapp.Modify-Nest-Presence-with-Hub-Status](https://github.com/dvanwinkle/smartapp.Modify-Nest-Presence-with-Hub-Status)

[dwiller11/SmartThings](https://github.com/dwiller11/SmartThings)

[eatonsn4/SmartthingsPrivate](https://github.com/eatonsn4/SmartthingsPrivate)

[eddgrant/smartthings-bulb-energy-publisher](https://github.com/eddgrant/smartthings-bulb-energy-publisher)

[ejluttmann/SmartThings](https://github.com/ejluttmann/SmartThings)

[eliotstocker/SmartThings-LightPhysicalControl](https://github.com/eliotstocker/SmartThings-LightPhysicalControl)

[eliotstocker/SmartThings-SweetLights-App](https://github.com/eliotstocker/SmartThings-SweetLights-App)

[emoses/front-door-handler](https://github.com/emoses/front-door-handler)

[emx2500/smartthings-auto-door-lock](https://github.com/emx2500/smartthings-auto-door-lock)

[enishoca/SmartHoca](https://github.com/enishoca/SmartHoca)

[enishoca/SmartThingsX](https://github.com/enishoca/SmartThingsX)

[eprzenic/arduinoSmartthingsHomeSecurity](https://github.com/eprzenic/arduinoSmartthingsHomeSecurity)

[ericcirone/smartthings](https://github.com/ericcirone/smartthings)

[ericmey/SmartThingsPublic](https://github.com/ericmey/SmartThingsPublic)

[ericvitale/ST-Average-Temperature-Trigger](https://github.com/ericvitale/ST-Average-Temperature-Trigger)

[ericvitale/ST-Home-Notify](https://github.com/ericvitale/ST-Home-Notify)

[ericvitale/ST-LIFX-Sync](https://github.com/ericvitale/ST-LIFX-Sync)

[ericvitale/ST-RebootRoku](https://github.com/ericvitale/ST-RebootRoku)

[ericvitale/ST-Revalver](https://github.com/ericvitale/ST-Revalver)

[ericvitale/ST-Toggimmer](https://github.com/ericvitale/ST-Toggimmer)

[ericvitale/ST-Toggle-Me](https://github.com/ericvitale/ST-Toggle-Me)

[ericvitale/ST-Trigger-My-Lights](https://github.com/ericvitale/ST-Trigger-My-Lights)

[erik4281/SmartThings](https://github.com/erik4281/SmartThings)

[erinel01/SmartThingsPublic](https://github.com/erinel01/SmartThingsPublic)

[erisod/heattape-smartthings](https://github.com/erisod/heattape-smartthings)

[evan/SmartThingsEvan](https://github.com/evan/SmartThingsEvan)

[evcallia/smartthings](https://github.com/evcallia/smartthings)

[eviljim/EviljimSmartThings](https://github.com/eviljim/EviljimSmartThings)

[fanelectronico/SmartThings-DoorLeftOpen](https://github.com/fanelectronico/SmartThings-DoorLeftOpen)

[fastdots/homeAutoWithST](https://github.com/fastdots/homeAutoWithST)

[fcp440/SmartThingsPublic](https://github.com/fcp440/SmartThingsPublic)

[fdlarsen/SmartThingsPublic](https://github.com/fdlarsen/SmartThingsPublic)

[felix-manea/st-smartapp-keep-heating-low-while-away](https://github.com/felix-manea/st-smartapp-keep-heating-low-while-away)

[fenfir/smart-apps](https://github.com/fenfir/smart-apps)

[feri113/SmartThingsPublic](https://github.com/feri113/SmartThingsPublic)

[findmory/smartthings-sensor_open_close](https://github.com/findmory/smartthings-sensor_open_close)

[fireboy1919/MiThings](https://github.com/fireboy1919/MiThings)

[fishy/SmartThings-OpenGarage](https://github.com/fishy/SmartThings-OpenGarage)

[fison67/GH-Connector](https://github.com/fison67/GH-Connector)

[fison67/HA-Connector](https://github.com/fison67/HA-Connector)

[fison67/LG-Connector](https://github.com/fison67/LG-Connector)

[fison67/TY-Connector](https://github.com/fison67/TY-Connector)

[fison67/WT-Connector](https://github.com/fison67/WT-Connector)

[fison67/mi_connector](https://github.com/fison67/mi_connector)

[flexembed/smartthings-apps](https://github.com/flexembed/smartthings-apps)

[flyerdp/flyerdp-SmartThings](https://github.com/flyerdp/flyerdp-SmartThings)

[flyjmz/jmzSmartThings](https://github.com/flyjmz/jmzSmartThings)

[fornever2/485-connector](https://github.com/fornever2/485-connector)

[frameloss/smart-blinkt](https://github.com/frameloss/smart-blinkt)

[freethewhat/mySmartThings](https://github.com/freethewhat/mySmartThings)

[frknhog/SmartThings](https://github.com/frknhog/SmartThings)

[fuzzysb/Garadget](https://github.com/fuzzysb/Garadget)

[fuzzysb/Hubitat](https://github.com/fuzzysb/Hubitat)

[fuzzysb/SmartThings](https://github.com/fuzzysb/SmartThings)

[fuzzysb/Tado](https://github.com/fuzzysb/Tado)

[fwadiver/Smartthings](https://github.com/fwadiver/Smartthings)

[fxstein/smartthings-smartlib](https://github.com/fxstein/smartthings-smartlib)

[gabrielstelmach/SmartThingsByGabriel](https://github.com/gabrielstelmach/SmartThingsByGabriel)

[gamviel/SmartThingsPublic](https://github.com/gamviel/SmartThingsPublic)

[garrywma/LightWaveRFSmartthingsLocalDeviceCreator](https://github.com/garrywma/LightWaveRFSmartthingsLocalDeviceCreator)

[garyrobert/SmartThings](https://github.com/garyrobert/SmartThings)

[gbonk/resume-on-vacant](https://github.com/gbonk/resume-on-vacant)

[gdoornink/SmartThings](https://github.com/gdoornink/SmartThings)

[getterdone/FlumeWaterMeter](https://github.com/getterdone/FlumeWaterMeter)

[gkl-sf/SmartThings](https://github.com/gkl-sf/SmartThings)

[gnomesoup/SmartThings](https://github.com/gnomesoup/SmartThings)

[godApinow/SmartThings](https://github.com/godApinow/SmartThings)

[gouldner/SmartThings-2](https://github.com/gouldner/SmartThings-2)

[gpete/SmartThings](https://github.com/gpete/SmartThings)

[gpetru1/smartthings](https://github.com/gpetru1/smartthings)

[gpzj/smartthings.smartapps](https://github.com/gpzj/smartthings.smartapps)

[greglarious/BlinkMotionHandler](https://github.com/greglarious/BlinkMotionHandler)

[grixxy/home_automation](https://github.com/grixxy/home_automation)

[grussr/custom-smartthings](https://github.com/grussr/custom-smartthings)

[gsteckman/rpi-rest](https://github.com/gsteckman/rpi-rest)

[guiambros/smartthings](https://github.com/guiambros/smartthings)

[guineau/SmartThingsPublic](https://github.com/guineau/SmartThingsPublic)

[gurase/SmartThings-Home-Assistant-Connect](https://github.com/gurase/SmartThings-Home-Assistant-Connect)

[haackr/SwitchOnWhenActive](https://github.com/haackr/SwitchOnWhenActive)

[haackr/SwitchRunsRoutine](https://github.com/haackr/SwitchRunsRoutine)

[hailbopp/MyRE](https://github.com/hailbopp/MyRE)

[halaszvarig/wemos-smartthings-integration](https://github.com/halaszvarig/wemos-smartthings-integration)

[hallomarco/SmartThingsPublic](https://github.com/hallomarco/SmartThingsPublic)

[harperreed/SmartThings-webhook](https://github.com/harperreed/SmartThings-webhook)

[harrisra/HarrisTribeSmart](https://github.com/harrisra/HarrisTribeSmart)

[harsha0867/SmartApp](https://github.com/harsha0867/SmartApp)

[hashneo/sentinel_smartthings](https://github.com/hashneo/sentinel_smartthings)

[hbaxi/SmartThingsPublic](https://github.com/hbaxi/SmartThingsPublic)

[herbcarroll/Smartthings](https://github.com/herbcarroll/Smartthings)

[hermskee/-The-hermskee-SmartThingsPublic](https://github.com/hermskee/-The-hermskee-SmartThingsPublic)

[holden86/mySmartthings](https://github.com/holden86/mySmartthings)

[homemations/SmartThings](https://github.com/homemations/SmartThings)

[hongkongkiwi/smartthings-august](https://github.com/hongkongkiwi/smartthings-august)

[hongtat/tasmota-connect](https://github.com/hongtat/tasmota-connect)

[hoobs-org/smartthings](https://github.com/hoobs-org/smartthings)

[hoskbreaker/SmartThingsSamsung](https://github.com/hoskbreaker/SmartThingsSamsung)

[howlermonkeys/SmartThingsPublic](https://github.com/howlermonkeys/SmartThingsPublic)

[husseinmohkhalil/MySmartthings](https://github.com/husseinmohkhalil/MySmartthings)

[hwornall/SmartThingsPublic](https://github.com/hwornall/SmartThingsPublic)

[iBeech/SmartThings](https://github.com/iBeech/SmartThings)

[iamstev/dehumidifier-helper](https://github.com/iamstev/dehumidifier-helper)

[iamstev/smartthings-iamstev-other](https://github.com/iamstev/smartthings-iamstev-other)

[iamstev/tv-times](https://github.com/iamstev/tv-times)

[ianisms/st-ianisms](https://github.com/ianisms/st-ianisms)

[ibm-watson-iot/gateway-smartthings](https://github.com/ibm-watson-iot/gateway-smartthings)

[ibwilbur/Old_SmartThingsPublic](https://github.com/ibwilbur/Old_SmartThingsPublic)

[idealerror/smartthings](https://github.com/idealerror/smartthings)

[idioffo89/SmartThings-3](https://github.com/idioffo89/SmartThings-3)

[ieguz/My-Smartthings](https://github.com/ieguz/My-Smartthings)

[ilangoodman/smartthings](https://github.com/ilangoodman/smartthings)

[imbrianj/oauth_controller](https://github.com/imbrianj/oauth_controller)

[infinityplusone/MySmartThings](https://github.com/infinityplusone/MySmartThings)

[infofiend/SwitchMania](https://github.com/infofiend/SwitchMania)

[invent81/SmartThingsGarageDoor](https://github.com/invent81/SmartThingsGarageDoor)

[iot-dsa-v2/dslink-java-v2-smartthings](https://github.com/iot-dsa-v2/dslink-java-v2-smartthings)

[iprak/SmartThings](https://github.com/iprak/SmartThings)

[iquix/AB-BLE-Presence](https://github.com/iquix/AB-BLE-Presence)

[irion7/BaywebSmartThings](https://github.com/irion7/BaywebSmartThings)

[irishhuggiebear/SmartThingsPublic-](https://github.com/irishhuggiebear/SmartThingsPublic-)

[isriam/smartthings-alarmserver](https://github.com/isriam/smartthings-alarmserver)

[itechuser/smartthings](https://github.com/itechuser/smartthings)

[itnting/smartthings](https://github.com/itnting/smartthings)

[jackrmanuel/SmartThingsPublic](https://github.com/jackrmanuel/SmartThingsPublic)

[jacksonsr/smartthings](https://github.com/jacksonsr/smartthings)

[jacobrichard/smartapp](https://github.com/jacobrichard/smartapp)

[jakekapellen/SmartThings](https://github.com/jakekapellen/SmartThings)

[jamesandariese/SmartThingsLIFXSceneButtons](https://github.com/jamesandariese/SmartThingsLIFXSceneButtons)

[jamesandariese/SmartThingsNeptuneApex](https://github.com/jamesandariese/SmartThingsNeptuneApex)

[jamiekowalczik/dash-smartthings](https://github.com/jamiekowalczik/dash-smartthings)

[jangellx/SmartApp-AutoOffice](https://github.com/jangellx/SmartApp-AutoOffice)

[jangellx/SmartApp-WasherDryerDone](https://github.com/jangellx/SmartApp-WasherDryerDone)

[jangellx/SmartApp-WaterHeaterSchedule](https://github.com/jangellx/SmartApp-WaterHeaterSchedule)

[jarrodmoss/wally-smartthings](https://github.com/jarrodmoss/wally-smartthings)

[jason0x43/homebridge-smartthings](https://github.com/jason0x43/homebridge-smartthings)

[jasonreid0873/SmartThings-LightwaverRF](https://github.com/jasonreid0873/SmartThings-LightwaverRF)

[jasonrwise77/My-SmartThings](https://github.com/jasonrwise77/My-SmartThings)

[jasperson/SmartThings](https://github.com/jasperson/SmartThings)

[jayreimers/yet-another-power-monitor](https://github.com/jayreimers/yet-another-power-monitor)

[jaysingleton/SmartThings-Sugar](https://github.com/jaysingleton/SmartThings-Sugar)

[jbasen/Crestron-SmartThings](https://github.com/jbasen/Crestron-SmartThings)

[jbestor/SmartThings](https://github.com/jbestor/SmartThings)

[jbestor/SmartThingsPublic](https://github.com/jbestor/SmartThingsPublic)

[jbienz/SmartThings](https://github.com/jbienz/SmartThings)

[jbishop123/SmartThingsPublic](https://github.com/jbishop123/SmartThingsPublic)

[jbtibor/SmartThings](https://github.com/jbtibor/SmartThings)

[jc214809/my-smartthings](https://github.com/jc214809/my-smartthings)

[jcCarroll/Test-SmartApp](https://github.com/jcCarroll/Test-SmartApp)

[jcCarroll/motion-nightlight](https://github.com/jcCarroll/motion-nightlight)

[jcbrooks92/SmartThingsCode-HomeAuto](https://github.com/jcbrooks92/SmartThingsCode-HomeAuto)

[jcharr1/smartthings-jcharr1](https://github.com/jcharr1/smartthings-jcharr1)

[jcholpuch/SmartThings](https://github.com/jcholpuch/SmartThings)

[jcrumley/SmartThings.MessageIfOpenTooLong](https://github.com/jcrumley/SmartThings.MessageIfOpenTooLong)

[jdetmold/JeffSmartThingsPersonal](https://github.com/jdetmold/JeffSmartThingsPersonal)

[jeanbeaulieu/Smartthings](https://github.com/jeanbeaulieu/Smartthings)

[jeanfredericplante/thermostat](https://github.com/jeanfredericplante/thermostat)

[jebbett/STHostPinger](https://github.com/jebbett/STHostPinger)

[jeberle5713/Smart-Garage-Door-Opener](https://github.com/jeberle5713/Smart-Garage-Door-Opener)

[jfpronovost/SmartThingsPublic](https://github.com/jfpronovost/SmartThingsPublic)

[jfrazx/STEight](https://github.com/jfrazx/STEight)

[jgagnon5541/SmartThingsPublic](https://github.com/jgagnon5541/SmartThingsPublic)

[jgorsica/st-smartapps](https://github.com/jgorsica/st-smartapps)

[jhaines0/CurbBridge](https://github.com/jhaines0/CurbBridge)

[jhaines0/HoneywellSecurity](https://github.com/jhaines0/HoneywellSecurity)

[jhansche/st-teslafi](https://github.com/jhansche/st-teslafi)

[jhench/mySmartthings](https://github.com/jhench/mySmartthings)

[jhstroebel/SmartThings-TCv1](https://github.com/jhstroebel/SmartThings-TCv1)

[jhstroebel/SmartThings-TCv2](https://github.com/jhstroebel/SmartThings-TCv2)

[jiayunhan/SmartAppAnalysis](https://github.com/jiayunhan/SmartAppAnalysis)

[jimbobdog/SmartThingsCode](https://github.com/jimbobdog/SmartThingsCode)

[jimdusseau/SmartApps](https://github.com/jimdusseau/SmartApps)

[jimguistwite/SmartStuff](https://github.com/jimguistwite/SmartStuff)

[jimmarks/SmartThingsPublic](https://github.com/jimmarks/SmartThingsPublic)

[jimmyfortinx/my-smartthings-repo](https://github.com/jimmyfortinx/my-smartthings-repo)

[jjensn/MiThings](https://github.com/jjensn/MiThings)

[jjhuff/SmartThings_RheemEcoNet](https://github.com/jjhuff/SmartThings_RheemEcoNet)

[jklutka/SmartThingsKlutkaAutomations](https://github.com/jklutka/SmartThingsKlutkaAutomations)

[jlukerdev/Switchy](https://github.com/jlukerdev/Switchy)

[jmarkwell/thermostat-manager](https://github.com/jmarkwell/thermostat-manager)

[jmaxxz/particle-smartthings](https://github.com/jmaxxz/particle-smartthings)

[jnewland/airfoil-api-smartthings](https://github.com/jnewland/airfoil-api-smartthings)

[jnguyenc/SmartThings-WebApp](https://github.com/jnguyenc/SmartThings-WebApp)

[joebeeson/SmartThings](https://github.com/joebeeson/SmartThings)

[joeharrison714/fpp-smartthings-app](https://github.com/joeharrison714/fpp-smartthings-app)

[joeltamkin/smartthings-ups](https://github.com/joeltamkin/smartthings-ups)

[johnvey/smartthings-hd-powerview](https://github.com/johnvey/smartthings-hd-powerview)

[johnwest80/Smartthings-To-X10-Smartapp](https://github.com/johnwest80/Smartthings-To-X10-Smartapp)

[jojanantony/SHMHelper](https://github.com/jojanantony/SHMHelper)

[jolivertx/SmartThingsPublic](https://github.com/jolivertx/SmartThingsPublic)

[jonbur/smartthings](https://github.com/jonbur/smartthings)

[jonhester/maquette](https://github.com/jonhester/maquette)

[jonscheiding/st-nest-monitor](https://github.com/jonscheiding/st-nest-monitor)

[jonscheiding/st-smartass-garage-door](https://github.com/jonscheiding/st-smartass-garage-door)

[jonscheiding/st-thirsty-flasher](https://github.com/jonscheiding/st-thirsty-flasher)

[jonscheiding/st-web-switches](https://github.com/jonscheiding/st-web-switches)

[jorgecis/Doortsy_SmartThings](https://github.com/jorgecis/Doortsy_SmartThings)

[jorgecis/NestAutoAway](https://github.com/jorgecis/NestAutoAway)

[jorgecis/Remotsy_SmartThings](https://github.com/jorgecis/Remotsy_SmartThings)

[jorgecis/Smartthings](https://github.com/jorgecis/Smartthings)

[josemh/SmartThings](https://github.com/josemh/SmartThings)

[josephbolus/SmartThingsPersonal](https://github.com/josephbolus/SmartThingsPersonal)

[joshjohnson/SmartThingsApps](https://github.com/joshjohnson/SmartThingsApps)

[joshs85/ST_TankUtility](https://github.com/joshs85/ST_TankUtility)

[joshualyon/ST-Kodi](https://github.com/joshualyon/ST-Kodi)

[joshualyon/STWinkRelay](https://github.com/joshualyon/STWinkRelay)

[joyfulhouse/AtticFanControl](https://github.com/joyfulhouse/AtticFanControl)

[joyfulhouse/OhmHour](https://github.com/joyfulhouse/OhmHour)

[jp-src/SmartThingsPublic](https://github.com/jp-src/SmartThingsPublic)

[jpullen88/SmartThingsPersonal](https://github.com/jpullen88/SmartThingsPersonal)

[jrhbcn/smartthings](https://github.com/jrhbcn/smartthings)

[jrhurley/smartthings](https://github.com/jrhurley/smartthings)

[jrlucier/idle-garage-door](https://github.com/jrlucier/idle-garage-door)

[jrwentz/SmartThingsSleepIQ](https://github.com/jrwentz/SmartThingsSleepIQ)

[jschlackman/MasterOutlet](https://github.com/jschlackman/MasterOutlet)

[jschnurr/az-smartthings-logger](https://github.com/jschnurr/az-smartthings-logger)

[jschollenberger/SmartThings](https://github.com/jschollenberger/SmartThings)

[jsconstantelos/SmartThings](https://github.com/jsconstantelos/SmartThings)

[jsconstantelos/jcdevhandlers](https://github.com/jsconstantelos/jcdevhandlers)

[jturner78/SmartThings](https://github.com/jturner78/SmartThings)

[juano2310/SmartThings_templates](https://github.com/juano2310/SmartThings_templates)

[julienjoannic/smartthings](https://github.com/julienjoannic/smartthings)

[jusa80/smartthings](https://github.com/jusa80/smartthings)

[justinarhodes/SmartThingsPublic](https://github.com/justinarhodes/SmartThingsPublic)

[justinlhudson/SmartThings](https://github.com/justinlhudson/SmartThings)

[justinmiller61/smartthings-someones-home](https://github.com/justinmiller61/smartthings-someones-home)

[justintime/SmartThings-at](https://github.com/justintime/SmartThings-at)

[jv-syntaxerror/smartthings-hid-edge](https://github.com/jv-syntaxerror/smartthings-hid-edge)

[jwoodrich/SmartThings-AmbientWeatherGateway](https://github.com/jwoodrich/SmartThings-AmbientWeatherGateway)

[jwoodrich/SmartThings-SleepIQ](https://github.com/jwoodrich/SmartThings-SleepIQ)

[jwoodrich/SmartThings-WeeWxWeatherStation](https://github.com/jwoodrich/SmartThings-WeeWxWeatherStation)

[jwsf/device-type.arduino-8-way-relay](https://github.com/jwsf/device-type.arduino-8-way-relay)

[jyen/SmartThings](https://github.com/jyen/SmartThings)

[jz2352/SmartThings](https://github.com/jz2352/SmartThings)

[kaptajnen/smartthings](https://github.com/kaptajnen/smartthings)

[katiebot/smartthings](https://github.com/katiebot/smartthings)

[kdanthony/garagedoor](https://github.com/kdanthony/garagedoor)

[kdorff/smartapps](https://github.com/kdorff/smartapps)

[kdurrance/SmartThingsRobotCleaning](https://github.com/kdurrance/SmartThingsRobotCleaning)

[kearygriffin/MySmarthings](https://github.com/kearygriffin/MySmarthings)

[kecorbin/rpi-smartthings](https://github.com/kecorbin/rpi-smartthings)

[kecorbin/smartthings](https://github.com/kecorbin/smartthings)

[keltymd/smartThingsPublic](https://github.com/keltymd/smartThingsPublic)

[kenobob/SmartThingsPublic](https://github.com/kenobob/SmartThingsPublic)

[kerbs17/smart-things](https://github.com/kerbs17/smart-things)

[keyurbhatnagar/nodegaragedoor](https://github.com/keyurbhatnagar/nodegaragedoor)

[kirkbrownOK/SensiThermostat](https://github.com/kirkbrownOK/SensiThermostat)

[kirkbrownOK/SevereWeatherAlertControlLights](https://github.com/kirkbrownOK/SevereWeatherAlertControlLights)

[kirkbrownOK/doorSensorDeMUX](https://github.com/kirkbrownOK/doorSensorDeMUX)

[kit-barnes/smartLEDdimmer](https://github.com/kit-barnes/smartLEDdimmer)

[kjonix/Smartthings](https://github.com/kjonix/Smartthings)

[klinquist/LightsOnWhenIGetHome](https://github.com/klinquist/LightsOnWhenIGetHome)

[klinquist/PushIfLightsLeftOn](https://github.com/klinquist/PushIfLightsLeftOn)

[kmcoulson/SmartThingsPanel](https://github.com/kmcoulson/SmartThingsPanel)

[kmorey/SmartThings](https://github.com/kmorey/SmartThings)

[kmsarabu/SmartThings](https://github.com/kmsarabu/SmartThings)

[kmugh/SmartThings](https://github.com/kmugh/SmartThings)

[konnected-io/konnected-security](https://github.com/konnected-io/konnected-security)

[konnected-io/noonlight-smartthings](https://github.com/konnected-io/noonlight-smartthings)

[konni/SmartThings](https://github.com/konni/SmartThings)

[korhadris/st_smart_fan](https://github.com/korhadris/st_smart_fan)

[koush/scrypted-smartthings](https://github.com/koush/scrypted-smartthings)

[kqyang/smartthings](https://github.com/kqyang/smartthings)

[kris-schaller/SmartApps-Community](https://github.com/kris-schaller/SmartApps-Community)

[krlaframboise/OtherHub2](https://github.com/krlaframboise/OtherHub2)

[krlaframboise/SmartThings](https://github.com/krlaframboise/SmartThings)

[krrupert5/SmartThingsApps](https://github.com/krrupert5/SmartThingsApps)

[ksainc/SmartThings](https://github.com/ksainc/SmartThings)

[kshenoy/SmartThings](https://github.com/kshenoy/SmartThings)

[kuestess/smartthings-insteonconnect](https://github.com/kuestess/smartthings-insteonconnect)

[kvwillis311/Smarthings-Devices](https://github.com/kvwillis311/Smarthings-Devices)

[kyGitHub411/SmartThingsPersonal](https://github.com/kyGitHub411/SmartThingsPersonal)

[kylesmith63/SmartThings](https://github.com/kylesmith63/SmartThings)

[langanjp/smartthings_jpl_public](https://github.com/langanjp/smartthings_jpl_public)

[lap33733/Smartthings](https://github.com/lap33733/Smartthings)

[lcsandman8301/SmartThingsIntegrations](https://github.com/lcsandman8301/SmartThingsIntegrations)

[leftmans/smartthings](https://github.com/leftmans/smartthings)

[lehighkid/ST-Collection](https://github.com/lehighkid/ST-Collection)

[leo212/tami4edge-smarthings-unofficial](https://github.com/leo212/tami4edge-smarthings-unofficial)

[leofig-rj/ST_WiLight](https://github.com/leofig-rj/ST_WiLight)

[leonschwartz/SmartThings](https://github.com/leonschwartz/SmartThings)

[lesterchan/smartthings](https://github.com/lesterchan/smartthings)

[lgkahn/hubitat](https://github.com/lgkahn/hubitat)

[liarjo/SmartThingsLab](https://github.com/liarjo/SmartThingsLab)

[liebman/SmartThings](https://github.com/liebman/SmartThings)

[lite2073/SmartThingsPublic-garyd9](https://github.com/lite2073/SmartThingsPublic-garyd9)

[ljbotero/Hubitat-SmartWaterHeater](https://github.com/ljbotero/Hubitat-SmartWaterHeater)

[llinder/smartapps](https://github.com/llinder/smartapps)

[lmosenk/Vacation-Lights-Director-ST-port](https://github.com/lmosenk/Vacation-Lights-Director-ST-port)

[lomarb/homebridge-smartthings-tonesto7](https://github.com/lomarb/homebridge-smartthings-tonesto7)

[loonass/ST-repo](https://github.com/loonass/ST-repo)

[loughmiller/sleepShiftSwitch](https://github.com/loughmiller/sleepShiftSwitch)

[louisjackson66/SmartThingsPublic](https://github.com/louisjackson66/SmartThingsPublic)

[loverso/SmartThingsDev](https://github.com/loverso/SmartThingsDev)

[luisfocosta/SmartThingsPublic](https://github.com/luisfocosta/SmartThingsPublic)

[luisfocosta/smartthings](https://github.com/luisfocosta/smartthings)

[lux4rd0/smartthings](https://github.com/lux4rd0/smartthings)

[m0ntecarloss/smartfan](https://github.com/m0ntecarloss/smartfan)

[m1cs/WaterHeater](https://github.com/m1cs/WaterHeater)

[maartenvantjonger/omnilogic-smartapp](https://github.com/maartenvantjonger/omnilogic-smartapp)

[mabelloc123/SmartThingsPublic](https://github.com/mabelloc123/SmartThingsPublic)

[macgngsta/groovy-autogreenhouse](https://github.com/macgngsta/groovy-autogreenhouse)

[macmedia/SmartThings](https://github.com/macmedia/SmartThings)

[macstainless/Dark-Weather](https://github.com/macstainless/Dark-Weather)

[madebyshanon/smartthings](https://github.com/madebyshanon/smartthings)

[madenwala/SmartThings-WindowMonitor](https://github.com/madenwala/SmartThings-WindowMonitor)

[magimat/Smartthings](https://github.com/magimat/Smartthings)

[magnusstam/SmartThings](https://github.com/magnusstam/SmartThings)

[makutaku/occupancy](https://github.com/makutaku/occupancy)

[maoten2000/SmartThings-1](https://github.com/maoten2000/SmartThings-1)

[mariussm/SmartThings-Apps](https://github.com/mariussm/SmartThings-Apps)

[mariussm/Telldus-SmartThings](https://github.com/mariussm/Telldus-SmartThings)

[mark-netalico/smartthings](https://github.com/mark-netalico/smartthings)

[markeadkins/IP-Temp-Humidity-Sensor---Smartthings](https://github.com/markeadkins/IP-Temp-Humidity-Sensor---Smartthings)

[marqelme/SmartThingsEnhanced](https://github.com/marqelme/SmartThingsEnhanced)

[martzcodes/SmartDSC](https://github.com/martzcodes/SmartDSC)

[matt-teix/SmartHome](https://github.com/matt-teix/SmartHome)

[mattPiratt/Smart-Things-eHome](https://github.com/mattPiratt/Smart-Things-eHome)

[mattbrain/Cytech-Comfort](https://github.com/mattbrain/Cytech-Comfort)

[mattemsley/SmartThings-MattEdits](https://github.com/mattemsley/SmartThings-MattEdits)

[mattglet/smartthings](https://github.com/mattglet/smartthings)

[mattsch/MySmartThings](https://github.com/mattsch/MySmartThings)

[mattw01/HubitatHarmony](https://github.com/mattw01/HubitatHarmony)

[mattw01/HubitatToSTNotificationPusher](https://github.com/mattw01/HubitatToSTNotificationPusher)

[mattw01/STBitBarApp](https://github.com/mattw01/STBitBarApp)

[mbarnathan/ESP8266-Wifi-Blinds](https://github.com/mbarnathan/ESP8266-Wifi-Blinds)

[mbarnathan/SmartWink](https://github.com/mbarnathan/SmartWink)

[mbarnathan/Synchronized-Dimming](https://github.com/mbarnathan/Synchronized-Dimming)

[mbeckner554/SmartThingsGarageDoor](https://github.com/mbeckner554/SmartThingsGarageDoor)

[mbeynon/SmartThings](https://github.com/mbeynon/SmartThings)

[mblack01/SmartThings](https://github.com/mblack01/SmartThings)

[mcavoya/garage-door-button](https://github.com/mcavoya/garage-door-button)

[mccorrym/smart-things](https://github.com/mccorrym/smart-things)

[mcdunning/PiGarageDoorController](https://github.com/mcdunning/PiGarageDoorController)

[mcgarryplace-michael/SmartThingsPublic](https://github.com/mcgarryplace-michael/SmartThingsPublic)

[mciastek/smartthings](https://github.com/mciastek/smartthings)

[meckelangelo/STFanLEDFixer](https://github.com/meckelangelo/STFanLEDFixer)

[meckelangelo/STHeaterACAutomation](https://github.com/meckelangelo/STHeaterACAutomation)

[merpius/SmartThings-merpius](https://github.com/merpius/SmartThings-merpius)

[mhatrey/TotalConnect](https://github.com/mhatrey/TotalConnect)

[mhempel247/smartthings](https://github.com/mhempel247/smartthings)

[micbase/smartthings](https://github.com/micbase/smartthings)

[mich013/iotdb-smartthings](https://github.com/mich013/iotdb-smartthings)

[michaelansel/UniFi-Controller-SmartThings](https://github.com/michaelansel/UniFi-Controller-SmartThings)

[michaelphilip/SmartThings](https://github.com/michaelphilip/SmartThings)

[midyear66/SmartApp-HVAC-circ-with-switch](https://github.com/midyear66/SmartApp-HVAC-circ-with-switch)

[midyear66/SmartApp-Limit-Temperature-Settings](https://github.com/midyear66/SmartApp-Limit-Temperature-Settings)

[midyear66/SmartApp-Poll-with-HTTP-GET](https://github.com/midyear66/SmartApp-Poll-with-HTTP-GET)

[mihamil/SmartThings_Hamilton](https://github.com/mihamil/SmartThings_Hamilton)

[mike-schiller/smartthings-button-routine-control](https://github.com/mike-schiller/smartthings-button-routine-control)

[mikea/smartthings-metrics](https://github.com/mikea/smartthings-metrics)

[mikee385/hubitat-mikee385](https://github.com/mikee385/hubitat-mikee385)

[mikee385/smartthings-mikee385](https://github.com/mikee385/smartthings-mikee385)

[mikepluta/MySmartThings](https://github.com/mikepluta/MySmartThings)

[mikuslaw/SmartthingsMqttService](https://github.com/mikuslaw/SmartthingsMqttService)

[milksteakmatt/STUPNP](https://github.com/milksteakmatt/STUPNP)

[minollo/SmartThingsPrivate](https://github.com/minollo/SmartThingsPrivate)

[minutillo/SmartThings](https://github.com/minutillo/SmartThings)

[mjkoster/SmartThings](https://github.com/mjkoster/SmartThings)

[mjr9804/smartthings](https://github.com/mjr9804/smartthings)

[mkrapivner/SmartThingsLegrand](https://github.com/mkrapivner/SmartThingsLegrand)

[mlasevich/SmartThingsMirrorSwitch](https://github.com/mlasevich/SmartThingsMirrorSwitch)

[mlosli/smartthings](https://github.com/mlosli/smartthings)

[mmunchinski/my_smartthings_apps](https://github.com/mmunchinski/my_smartthings_apps)

[mobious12/SmartThings](https://github.com/mobious12/SmartThings)

[mokuso/SmartThings](https://github.com/mokuso/SmartThings)

[mparentes/smartthings](https://github.com/mparentes/smartthings)

[mrgw/Smartthings](https://github.com/mrgw/Smartthings)

[mrjwhite/homeflow-smartapp](https://github.com/mrjwhite/homeflow-smartapp)

[mrmoorey/Control-Maximum-Volume](https://github.com/mrmoorey/Control-Maximum-Volume)

[mrmoorey/Pause-Music-When-Motion-Stops](https://github.com/mrmoorey/Pause-Music-When-Motion-Stops)

[mrnohr/my-smartapps](https://github.com/mrnohr/my-smartapps)

[mrviper100/SmartThings](https://github.com/mrviper100/SmartThings)

[msteinma/MySmartThings](https://github.com/msteinma/MySmartThings)

[mtbeaver/SmartThingsDev](https://github.com/mtbeaver/SmartThingsDev)

[murphybrendan/smartthings_logger](https://github.com/murphybrendan/smartthings_logger)

[mvevitsis/helloworld](https://github.com/mvevitsis/helloworld)

[mvgrimes/smartthings-multi-thermostat](https://github.com/mvgrimes/smartthings-multi-thermostat)

[mvgrimes/smartthings-super-sprinker](https://github.com/mvgrimes/smartthings-super-sprinker)

[mwren/st-datalogger](https://github.com/mwren/st-datalogger)

[mwstowe/kumo](https://github.com/mwstowe/kumo)

[mxrugg/SmartThings](https://github.com/mxrugg/SmartThings)

[n3pjk/Knockerz](https://github.com/n3pjk/Knockerz)

[n8xd/AskHome](https://github.com/n8xd/AskHome)

[napalmcsr/SmartThingsStuff](https://github.com/napalmcsr/SmartThingsStuff)

[nassereb/SmartThings](https://github.com/nassereb/SmartThings)

[natalan/SmartThings](https://github.com/natalan/SmartThings)

[natalan/things-export](https://github.com/natalan/things-export)

[natecj/SmartThings](https://github.com/natecj/SmartThings)

[natekspencer/BwaSpaManager](https://github.com/natekspencer/BwaSpaManager)

[natekspencer/LitterRobotManager](https://github.com/natekspencer/LitterRobotManager)

[natekspencer/VivintSmartHomeManager](https://github.com/natekspencer/VivintSmartHomeManager)

[nateober/SmartThings](https://github.com/nateober/SmartThings)

[nathangroom/SmartApps](https://github.com/nathangroom/SmartApps)

[nd0905/SmartThings](https://github.com/nd0905/SmartThings)

[ndhunay/smartthings](https://github.com/ndhunay/smartthings)

[ndroo/smartthings](https://github.com/ndroo/smartthings)

[neallclark/smartthings](https://github.com/neallclark/smartthings)

[nemario/SmartThings-mystuff](https://github.com/nemario/SmartThings-mystuff)

[neophenix/STCirconusClimateTrap](https://github.com/neophenix/STCirconusClimateTrap)

[neophenix/STTurnOffMyLights](https://github.com/neophenix/STTurnOffMyLights)

[nevdull77/SmartThings](https://github.com/nevdull77/SmartThings)

[neville-nazerane/smarthub](https://github.com/neville-nazerane/smarthub)

[ng-family/SmartThings-Related](https://github.com/ng-family/SmartThings-Related)

[nicholaswilde/smartthings](https://github.com/nicholaswilde/smartthings)

[nicjansma/smart-things](https://github.com/nicjansma/smart-things)

[nmmacmillan/smartthings](https://github.com/nmmacmillan/smartthings)

[nocrosses/link-things](https://github.com/nocrosses/link-things)

[noname4444/smartthings](https://github.com/noname4444/smartthings)

[north3221/north3221SmartThings](https://github.com/north3221/north3221SmartThings)

[norxz7777/SmartThingsPublic](https://github.com/norxz7777/SmartThingsPublic)

[nsslabcuus/IoTMon](https://github.com/nsslabcuus/IoTMon)

[nutechsoftware/alarmdecoder-smartthings](https://github.com/nutechsoftware/alarmdecoder-smartthings)

[nuttytree/Nutty-SmartThings](https://github.com/nuttytree/Nutty-SmartThings)

[oariel/SmartThingsPublic](https://github.com/oariel/SmartThingsPublic)

[obmaz/govee_dth](https://github.com/obmaz/govee_dth)

[obycode/BeaconThings](https://github.com/obycode/BeaconThings)

[obycode/obything](https://github.com/obycode/obything)

[ocdc/SmartThings](https://github.com/ocdc/SmartThings)

[octoblu/octoblu-smartapp](https://github.com/octoblu/octoblu-smartapp)

[oilerfan21/Smartthings](https://github.com/oilerfan21/Smartthings)

[oliversierrab/SmartApps](https://github.com/oliversierrab/SmartApps)

[oliverspryn/smartthings-arduino-message-sender](https://github.com/oliverspryn/smartthings-arduino-message-sender)

[oliverspryn/smartthings-mirror](https://github.com/oliverspryn/smartthings-mirror)

[onarvaez3/SmartThings_DoorLightKnock](https://github.com/onarvaez3/SmartThings_DoorLightKnock)

[oneshotsink/smartthings](https://github.com/oneshotsink/smartthings)

[orandasoft/SmartThings](https://github.com/orandasoft/SmartThings)

[orateam/smartthings](https://github.com/orateam/smartthings)

[orecus/smartthings](https://github.com/orecus/smartthings)

[oschrich/vesync-smartthings](https://github.com/oschrich/vesync-smartthings)

[osimanager/smartapps](https://github.com/osimanager/smartapps)

[osteele/highland-smartapp](https://github.com/osteele/highland-smartapp)

[osvadimos/smartthings](https://github.com/osvadimos/smartthings)

[otaviojr/node-red-contrib-smartthings](https://github.com/otaviojr/node-red-contrib-smartthings)

[oukene/smart-switch](https://github.com/oukene/smart-switch)

[ovonick/SmartThingsOvonick](https://github.com/ovonick/SmartThingsOvonick)

[pakmanwg/smartthings-thermostat-timer](https://github.com/pakmanwg/smartthings-thermostat-timer)

[panealy/SmartThings](https://github.com/panealy/SmartThings)

[paravibe/smartthings](https://github.com/paravibe/smartthings)

[paruljain/smartthings](https://github.com/paruljain/smartthings)

[patrickkpowell/SmartThingsPublic](https://github.com/patrickkpowell/SmartThingsPublic)

[paul591/SmartThings](https://github.com/paul591/SmartThings)

[paulsheldon/SmartThings-PS](https://github.com/paulsheldon/SmartThings-PS)

[pavel-dpa/smartthings](https://github.com/pavel-dpa/smartthings)

[pbalogh77/smartthings](https://github.com/pbalogh77/smartthings)

[pbianco/SmartThingsPublic](https://github.com/pbianco/SmartThingsPublic)

[pcartwright81/pcartwright81-SmartThings](https://github.com/pcartwright81/pcartwright81-SmartThings)

[pepemontana7/smartthings](https://github.com/pepemontana7/smartthings)

[petermajor/SmartThings](https://github.com/petermajor/SmartThings)

[peternixon/SmartThings-PeterNixon](https://github.com/peternixon/SmartThings-PeterNixon)

[phetherton/smartapps](https://github.com/phetherton/smartapps)

[phflugre/smartthings-tesla](https://github.com/phflugre/smartthings-tesla)

[philh30/smartthings](https://github.com/philh30/smartthings)

[philippegravel/SmartThingsPublic](https://github.com/philippegravel/SmartThingsPublic)

[philippeportesppo/AirMentorPro2_SmartThings](https://github.com/philippeportesppo/AirMentorPro2_SmartThings)

[philippeportesppo/Dump_Temperature_Battery_Level_SmartThings](https://github.com/philippeportesppo/Dump_Temperature_Battery_Level_SmartThings)

[philippeportesppo/HumidityLightControl](https://github.com/philippeportesppo/HumidityLightControl)

[philippeportesppo/Smartapp](https://github.com/philippeportesppo/Smartapp)

[philippeportesppo/WeatherUndergroundWeb_SmartThings](https://github.com/philippeportesppo/WeatherUndergroundWeb_SmartThings)

[phiz118/smartthings](https://github.com/phiz118/smartthings)

[phreezee/SmartThingsPublic](https://github.com/phreezee/SmartThingsPublic)

[physhster/Vision-ZL7432](https://github.com/physhster/Vision-ZL7432)

[piXelPoivre/smartthings-ipx800](https://github.com/piXelPoivre/smartthings-ipx800)

[pjoyce42/limitlessled-smartthings-local-control](https://github.com/pjoyce42/limitlessled-smartthings-local-control)

[pkananen/SmartThingsApps](https://github.com/pkananen/SmartThingsApps)

[pklokke/SmartThings-WiserSmartDevices](https://github.com/pklokke/SmartThings-WiserSmartDevices)

[pkmindworks/PKSmartThings](https://github.com/pkmindworks/PKSmartThings)

[pmckinnon/SmartThingsPublic](https://github.com/pmckinnon/SmartThingsPublic)

[pmjoen/SmartThingsMjoen](https://github.com/pmjoen/SmartThingsMjoen)

[pmlab-ucd/SmartAppAnalyzer](https://github.com/pmlab-ucd/SmartAppAnalyzer)

[pocc/toggl_smartapp](https://github.com/pocc/toggl_smartapp)

[poindexter12/arduino-smartthings-relay](https://github.com/poindexter12/arduino-smartthings-relay)

[project802/smartthings](https://github.com/project802/smartthings)

[psecto/SamsungSmartThings](https://github.com/psecto/SamsungSmartThings)

[pstuart/HousePanel](https://github.com/pstuart/HousePanel)

[pstuart/smartthings-ps](https://github.com/pstuart/smartthings-ps)

[psun03/SmartThings](https://github.com/psun03/SmartThings)

[ptoledo/SmartThingsPersonal](https://github.com/ptoledo/SmartThingsPersonal)

[pvanbaren/SmartThings](https://github.com/pvanbaren/SmartThings)

[qJake/SmartThings-Actions](https://github.com/qJake/SmartThings-Actions)

[quielb/MySmartThings](https://github.com/quielb/MySmartThings)

[r351574nc3/smartthings_hackathon](https://github.com/r351574nc3/smartthings_hackathon)

[r3dey3/st-wifi-devices](https://github.com/r3dey3/st-wifi-devices)

[rafa400/SmartThingsPublic](https://github.com/rafa400/SmartThingsPublic)

[rafaelborja/smartthings](https://github.com/rafaelborja/smartthings)

[ragross/SmartThingsPublic](https://github.com/ragross/SmartThingsPublic)

[randymxj/AutomaticToSmartthings](https://github.com/randymxj/AutomaticToSmartthings)

[rasmusagdestein/MySmartThings](https://github.com/rasmusagdestein/MySmartThings)

[raven42/smartthings](https://github.com/raven42/smartthings)

[rayzurbock/SmartThings-BigTalker-Orig](https://github.com/rayzurbock/SmartThings-BigTalker-Orig)

[rayzurbock/SmartThings-DoorLeftOpen](https://github.com/rayzurbock/SmartThings-DoorLeftOpen)

[rayzurbock/SmartThings-LightControlViaMotion](https://github.com/rayzurbock/SmartThings-LightControlViaMotion)

[rayzurbock/SmartThings-home-on-valid-code-unlock](https://github.com/rayzurbock/SmartThings-home-on-valid-code-unlock)

[rboy1/smartthings-1](https://github.com/rboy1/smartthings-1)

[rcoodey/Pilarm](https://github.com/rcoodey/Pilarm)

[rcoodey/Piputer](https://github.com/rcoodey/Piputer)

[reclusivellama/AdultSmartThings](https://github.com/reclusivellama/AdultSmartThings)

[redloro/smartthings](https://github.com/redloro/smartthings)

[rednus/SmartThingsPublic](https://github.com/rednus/SmartThingsPublic)

[reneric/SmartThingsPublic](https://github.com/reneric/SmartThingsPublic)

[rhyolight/smartthings-apps](https://github.com/rhyolight/smartthings-apps)

[riaanbotes/smartthings-datastore-collector](https://github.com/riaanbotes/smartthings-datastore-collector)

[richardwhite00/rw-st_anything](https://github.com/richardwhite00/rw-st_anything)

[rjsmoffat/smartthingsstuff](https://github.com/rjsmoffat/smartthingsstuff)

[rkroboth/GarageDoorOpenAlert](https://github.com/rkroboth/GarageDoorOpenAlert)

[rkroboth/smartthings_lan_service_manager](https://github.com/rkroboth/smartthings_lan_service_manager)

[rleonard55/rleonard-SmartThings](https://github.com/rleonard55/rleonard-SmartThings)

[rlipenta/alarm-monitor](https://github.com/rlipenta/alarm-monitor)

[rllynch/pi_garage_smartthings](https://github.com/rllynch/pi_garage_smartthings)

[rllynch/smartthings_cli](https://github.com/rllynch/smartthings_cli)

[rllynch/smartthings_unlock_on_arrival](https://github.com/rllynch/smartthings_unlock_on_arrival)

[robbiet480/SmartThingsThings](https://github.com/robbiet480/SmartThingsThings)

[robbybx/PerezSmartApp](https://github.com/robbybx/PerezSmartApp)

[robertwww/SmartThings](https://github.com/robertwww/SmartThings)

[robfarmergt/smartthings-phrasebutton](https://github.com/robfarmergt/smartthings-phrasebutton)

[roblandry/SmartThings](https://github.com/roblandry/SmartThings)

[robmcd527/SmartThings](https://github.com/robmcd527/SmartThings)

[robrtb/custom-smartthings](https://github.com/robrtb/custom-smartthings)

[roddenshaw/smartthings](https://github.com/roddenshaw/smartthings)

[rodneyrowen/RarSmartThings](https://github.com/rodneyrowen/RarSmartThings)

[rodtoll/smartthings-isy](https://github.com/rodtoll/smartthings-isy)

[rodtoll/smartthings-swann-nvr](https://github.com/rodtoll/smartthings-swann-nvr)

[rogersmj/st-door-alert-after-dark](https://github.com/rogersmj/st-door-alert-after-dark)

[rosswerks/SmartThings](https://github.com/rosswerks/SmartThings)

[rrazor/power-state-duration](https://github.com/rrazor/power-state-duration)

[rtakacs/iotjs-smartthings-demo](https://github.com/rtakacs/iotjs-smartthings-demo)

[rtorchia/DSC-Envisalink](https://github.com/rtorchia/DSC-Envisalink)

[rtorchia/Smartthings-Hikvision-Events](https://github.com/rtorchia/Smartthings-Hikvision-Events)

[rtorchia/rti_audio](https://github.com/rtorchia/rti_audio)

[rtyle/fortrezz](https://github.com/rtyle/fortrezz)

[rtyle/heat-water-at-sinks](https://github.com/rtyle/heat-water-at-sinks)

[rtyle/light-the-way](https://github.com/rtyle/light-the-way)

[rtyle/reflections](https://github.com/rtyle/reflections)

[rtyle/set-input-source](https://github.com/rtyle/set-input-source)

[rtyle/upnp-connect](https://github.com/rtyle/upnp-connect)

[rtyle/wake-on-lan](https://github.com/rtyle/wake-on-lan)

[rwhapham/smartthings](https://github.com/rwhapham/smartthings)

[ryanez5/CoffeeBar](https://github.com/ryanez5/CoffeeBar)

[safetyguy14/smartthings](https://github.com/safetyguy14/smartthings)

[sagarxooa/samples](https://github.com/sagarxooa/samples)

[sajiko5821/Home-Automations](https://github.com/sajiko5821/Home-Automations)

[samuelkadolph/smart-home](https://github.com/samuelkadolph/smart-home)

[sandro2304/SmartThingsPublic](https://github.com/sandro2304/SmartThingsPublic)

[sashamobilesm/paulsheldon-SmartThings-PS](https://github.com/sashamobilesm/paulsheldon-SmartThings-PS)

[sbdobrescu/SmartThings](https://github.com/sbdobrescu/SmartThings)

[scottgulliver/smartthings-pi](https://github.com/scottgulliver/smartthings-pi)

[scrampker/Club-Steve-Auto-Lock](https://github.com/scrampker/Club-Steve-Auto-Lock)

[seldonsmule/smartthings-heated-mats-by-temp](https://github.com/seldonsmule/smartthings-heated-mats-by-temp)

[sethaniel/SethsSmartThings](https://github.com/sethaniel/SethsSmartThings)

[sgnagnarella/Smartthings_TotalConnect_SmartApp](https://github.com/sgnagnarella/Smartthings_TotalConnect_SmartApp)

[sgupta999/GuptaSmartthingsRepository](https://github.com/sgupta999/GuptaSmartthingsRepository)

[sgupta999/gupta-st-devices-smartapps](https://github.com/sgupta999/gupta-st-devices-smartapps)

[shackrat/SmartThings-by-Shackrat](https://github.com/shackrat/SmartThings-by-Shackrat)

[shadowjig/Stoplight](https://github.com/shadowjig/Stoplight)

[shamlian/SmartThings](https://github.com/shamlian/SmartThings)

[shamlian/SmartThingsExperiments](https://github.com/shamlian/SmartThingsExperiments)

[shaunsbennett/SmartThingsCollection](https://github.com/shaunsbennett/SmartThingsCollection)

[sheikhsphere/SmartApp-smart-humidifier-dehumidifier](https://github.com/sheikhsphere/SmartApp-smart-humidifier-dehumidifier)

[shepner/SmartThings](https://github.com/shepner/SmartThings)

[shinznatkid/Samsung-PowerBot-Vacuum-Fixed](https://github.com/shinznatkid/Samsung-PowerBot-Vacuum-Fixed)

[showpointer/Smartthings](https://github.com/showpointer/Smartthings)

[sicross/smartthings](https://github.com/sicross/smartthings)

[sidjohn1/hubitat](https://github.com/sidjohn1/hubitat)

[siivv/smartthings](https://github.com/siivv/smartthings)

[simba2wang/SmartThings.AverageThings](https://github.com/simba2wang/SmartThings.AverageThings)

[simshengqin/SmartThings_Server](https://github.com/simshengqin/SmartThings_Server)

[slashrjl/SmartThings](https://github.com/slashrjl/SmartThings)

[slonob/homethings](https://github.com/slonob/homethings)

[slushpupie/MySmartThings](https://github.com/slushpupie/MySmartThings)

[smartthings-users/smartapp.off-without-motion](https://github.com/smartthings-users/smartapp.off-without-motion)

[smartthings-users/smartapp.security-night-light](https://github.com/smartthings-users/smartapp.security-night-light)

[smeisner/SmartThings](https://github.com/smeisner/SmartThings)

[snailium/SmartThings_Multi_Way_Switch](https://github.com/snailium/SmartThings_Multi_Way_Switch)

[snakedog116/SmartThings-Snakedog116](https://github.com/snakedog116/SmartThings-Snakedog116)

[snike3/SmartThings-Random-Tinkery](https://github.com/snike3/SmartThings-Random-Tinkery)

[snowake4me/smartthings-api-smartapp](https://github.com/snowake4me/smartthings-api-smartapp)

[spapadim/smartthings](https://github.com/spapadim/smartthings)

[speteor/Code-Examples-For-Build](https://github.com/speteor/Code-Examples-For-Build)

[spindance/spindance.imp-smartthings.bridge](https://github.com/spindance/spindance.imp-smartthings.bridge)

[spinn360/Smartthings](https://github.com/spinn360/Smartthings)

[spoonsautomation/smartapps](https://github.com/spoonsautomation/smartapps)

[sripraneeth/SmartThings](https://github.com/sripraneeth/SmartThings)

[srmooney/SmartThings](https://github.com/srmooney/SmartThings)

[srpape/fishtank](https://github.com/srpape/fishtank)

[sshakoor/SmartThingsPublic](https://github.com/sshakoor/SmartThingsPublic)

[sshort/sshort-smartthings](https://github.com/sshort/sshort-smartthings)

[ssilence5/Simplisafe-SmartThings](https://github.com/ssilence5/Simplisafe-SmartThings)

[st-swanny/smartthings](https://github.com/st-swanny/smartthings)

[stanculescum/aplicatii-SmartThings](https://github.com/stanculescum/aplicatii-SmartThings)

[star114/Smartthings](https://github.com/star114/Smartthings)

[staugaard/SmartThings-Carwings](https://github.com/staugaard/SmartThings-Carwings)

[stelpro/UpdateWeather_SmartApp](https://github.com/stelpro/UpdateWeather_SmartApp)

[stephaneminisini/smartthings](https://github.com/stephaneminisini/smartthings)

[steve-gregory/irobot-manager](https://github.com/steve-gregory/irobot-manager)

[steve28/SmartThings](https://github.com/steve28/SmartThings)

[stevecoug/smartapps](https://github.com/stevecoug/smartapps)

[stevenhaddox/SmartThings-Custom](https://github.com/stevenhaddox/SmartThings-Custom)

[sticks18/Fibaro-RGBW-Group-Controller](https://github.com/sticks18/Fibaro-RGBW-Group-Controller)

[sticks18/Lightify-Bulb](https://github.com/sticks18/Lightify-Bulb)

[sticks18/Stateless-Scene-via-Echo](https://github.com/sticks18/Stateless-Scene-via-Echo)

[strelitzia123/SmartThings](https://github.com/strelitzia123/SmartThings)

[strifejester/smartthingsapps](https://github.com/strifejester/smartthingsapps)

[summitbri/smartthings-customization](https://github.com/summitbri/smartthings-customization)

[surge919/SmartThings-Surge919-2](https://github.com/surge919/SmartThings-Surge919-2)

[surge919/SmartThings-surge919](https://github.com/surge919/SmartThings-surge919)

[swamplynx/SmartThingsGroovy](https://github.com/swamplynx/SmartThingsGroovy)

[synfinatic/SmartThings](https://github.com/synfinatic/SmartThings)

[t-royx/SEIS744-Final](https://github.com/t-royx/SEIS744-Final)

[tamaracks/smartthings-other](https://github.com/tamaracks/smartthings-other)

[tamttt/Smartthings-Arduino-RF-Ceiling-Fan](https://github.com/tamttt/Smartthings-Arduino-RF-Ceiling-Fan)

[tamttt/Smartthings-Arduino-RF-Ceiling-Fan-with-Etekcity-Switch](https://github.com/tamttt/Smartthings-Arduino-RF-Ceiling-Fan-with-Etekcity-Switch)

[tbeseda/smartthings-auth-example](https://github.com/tbeseda/smartthings-auth-example)

[tcjennings/SmartThingsPrivate](https://github.com/tcjennings/SmartThingsPrivate)

[techgaun/xmart-things-demo](https://github.com/techgaun/xmart-things-demo)

[technothingy/LGTVNotification](https://github.com/technothingy/LGTVNotification)

[techwithjake/smartthings-custom](https://github.com/techwithjake/smartthings-custom)

[tekathome/smartthings](https://github.com/tekathome/smartthings)

[tenstartups/smartthings-dsc-bridge](https://github.com/tenstartups/smartthings-dsc-bridge)

[tenstartups/smartthings-isy-bridge](https://github.com/tenstartups/smartthings-isy-bridge)

[terickson/SmartThingsPublic](https://github.com/terickson/SmartThingsPublic)

[teropikala/smartthings-switch-slave](https://github.com/teropikala/smartthings-switch-slave)

[terry2012/STAnalyzer](https://github.com/terry2012/STAnalyzer)

[terumorimura/SmartThings](https://github.com/terumorimura/SmartThings)

[teuteuguy/Cloud-Based_TP-Link-to-SmartThings-Integration](https://github.com/teuteuguy/Cloud-Based_TP-Link-to-SmartThings-Integration)

[tfatykhov/WinkRedNode](https://github.com/tfatykhov/WinkRedNode)

[tfrevert/SmartThings_Humidity_Control](https://github.com/tfrevert/SmartThings_Humidity_Control)

[tfrevert/SmartThings_Numerous_Integration](https://github.com/tfrevert/SmartThings_Numerous_Integration)

[tguerena/SmartThings](https://github.com/tguerena/SmartThings)

[thadd/smartthings](https://github.com/thadd/smartthings)

[the1snm/SmartThingsDTH](https://github.com/the1snm/SmartThingsDTH)

[thedoughill/st-hill](https://github.com/thedoughill/st-hill)

[thegilbertchan/SmartThings-1](https://github.com/thegilbertchan/SmartThings-1)

[therkilt/SmartThings-Projects](https://github.com/therkilt/SmartThings-Projects)

[thermalatom1/SmartThingsDSCAlarm](https://github.com/thermalatom1/SmartThingsDSCAlarm)

[thi517/SmartThingsPublic](https://github.com/thi517/SmartThingsPublic)

[thomasruns/know-lock-status](https://github.com/thomasruns/know-lock-status)

[thomasruns/smartthings-monitor-power-meter](https://github.com/thomasruns/smartthings-monitor-power-meter)

[thrashernb/smartthings-9](https://github.com/thrashernb/smartthings-9)

[tiancu1980/SmartThings](https://github.com/tiancu1980/SmartThings)

[tibber/tibber-smartthings-app](https://github.com/tibber/tibber-smartthings-app)

[tierneykev/SmartThings_New](https://github.com/tierneykev/SmartThings_New)

[ties1980/Sonos-controller-by-Zigbee-Dimmer---Samsung-Smartthings](https://github.com/ties1980/Sonos-controller-by-Zigbee-Dimmer---Samsung-Smartthings)

[tillig/SmartThings](https://github.com/tillig/SmartThings)

[timmayforeman/SmartThingsPublic](https://github.com/timmayforeman/SmartThingsPublic)

[tinypocket/SmartThings](https://github.com/tinypocket/SmartThings)

[tituswoo/homeflow-smartapp](https://github.com/tituswoo/homeflow-smartapp)

[tmdevries/HomeAutomationWithST](https://github.com/tmdevries/HomeAutomationWithST)

[tmleafs/life360-smartthings-refresh](https://github.com/tmleafs/life360-smartthings-refresh)

[tmleafs/smartthingsdoorbell](https://github.com/tmleafs/smartthingsdoorbell)

[tmlong/SmartThings](https://github.com/tmlong/SmartThings)

[toddtriv/smartthings](https://github.com/toddtriv/smartthings)

[tomagoes/STprojects](https://github.com/tomagoes/STprojects)

[tomasaxerot/SmartThings](https://github.com/tomasaxerot/SmartThings)

[tomforti/Ecovent](https://github.com/tomforti/Ecovent)

[tomforti/Keeping-it-Cool](https://github.com/tomforti/Keeping-it-Cool)

[tomforti/MySmartthings](https://github.com/tomforti/MySmartthings)

[tomriv77/stapp_autolock](https://github.com/tomriv77/stapp_autolock)

[tomriv77/stapp_water_power_monitor](https://github.com/tomriv77/stapp_water_power_monitor)

[tonesto7/SmartThings-tonesto7-public](https://github.com/tonesto7/SmartThings-tonesto7-public)

[tonesto7/homebridge-smartthings-tonesto7](https://github.com/tonesto7/homebridge-smartthings-tonesto7)

[tonesto7/nest-manager](https://github.com/tonesto7/nest-manager)

[tonysebion/SmartThings](https://github.com/tonysebion/SmartThings)

[topshelf-code/Smartthings-Custom-Alarm-with-Entry-Exit-Delay](https://github.com/topshelf-code/Smartthings-Custom-Alarm-with-Entry-Exit-Delay)

[tracmo/quantum_xim_light_smartthings](https://github.com/tracmo/quantum_xim_light_smartthings)

[tracmo/quantum_xim_plug_smartthings](https://github.com/tracmo/quantum_xim_plug_smartthings)

[travcam/smart_trends](https://github.com/travcam/smart_trends)

[treimer1/smartthings](https://github.com/treimer1/smartthings)

[trentfoley64/SmartThings](https://github.com/trentfoley64/SmartThings)

[treythomas123/smartthings-remote-hue](https://github.com/treythomas123/smartthings-remote-hue)

[triosniolin/smartthings](https://github.com/triosniolin/smartthings)

[tronikos/FlumeSmartThings](https://github.com/tronikos/FlumeSmartThings)

[turlvo/KuKuHarmony](https://github.com/turlvo/KuKuHarmony)

[turlvo/KuKuMeter](https://github.com/turlvo/KuKuMeter)

[turlvo/KuKuMi](https://github.com/turlvo/KuKuMi)

[twack/Quirky-Connect](https://github.com/twack/Quirky-Connect)

[twisty/PossiblySmartThings](https://github.com/twisty/PossiblySmartThings)

[tybo27/SmartThingsPersonal](https://github.com/tybo27/SmartThingsPersonal)

[tylerfreckmann/smartthings](https://github.com/tylerfreckmann/smartthings)

[tynet94/SmartThingsSegmentLogger](https://github.com/tynet94/SmartThingsSegmentLogger)

[tyoung3/BlinkLight](https://github.com/tyoung3/BlinkLight)

[tyuhl/SmartThings](https://github.com/tyuhl/SmartThings)

[utamir/khome](https://github.com/utamir/khome)

[venumx/SmartThingsConnector](https://github.com/venumx/SmartThingsConnector)

[verbem/Domoticz-Server](https://github.com/verbem/Domoticz-Server)

[vervallsweg/smartthings](https://github.com/vervallsweg/smartthings)

[vikashvarma/SmartThings](https://github.com/vikashvarma/SmartThings)

[vikashvarma/smartThings-Thermostat](https://github.com/vikashvarma/smartThings-Thermostat)

[vonrandy/SmartThings](https://github.com/vonrandy/SmartThings)

[voodoojello/smartthings](https://github.com/voodoojello/smartthings)

[vzakharchenko/smartthings-phevctl](https://github.com/vzakharchenko/smartthings-phevctl)

[vzakharchenko/smartthings-phone-presence-sensor](https://github.com/vzakharchenko/smartthings-phone-presence-sensor)

[vzakharchenko/smartthings-sonoff](https://github.com/vzakharchenko/smartthings-sonoff)

[wallbasher/SmartPiGarageDoor](https://github.com/wallbasher/SmartPiGarageDoor)

[warrenhallen/smartthingspublic](https://github.com/warrenhallen/smartthingspublic)

[wbrussell/MySmartThings](https://github.com/wbrussell/MySmartThings)

[wcleafs2002/SmartThingsPublic](https://github.com/wcleafs2002/SmartThingsPublic)

[weikai/WeikaiSmartThings](https://github.com/weikai/WeikaiSmartThings)

[whatificould/SmartThingsIRBridgeLCD](https://github.com/whatificould/SmartThingsIRBridgeLCD)

[windsurfer99/ST_StreamLabs-Water-Flow](https://github.com/windsurfer99/ST_StreamLabs-Water-Flow)

[wjarrettc/smartapps](https://github.com/wjarrettc/smartapps)

[wyattearp/STBatteryMonitor](https://github.com/wyattearp/STBatteryMonitor)

[xdumaine/smartthings-auto-close-garage-doors](https://github.com/xdumaine/smartthings-auto-close-garage-doors)

[xdumaine/smartthings-autolock-doors](https://github.com/xdumaine/smartthings-autolock-doors)

[xnetdude/SmartThingsPublic](https://github.com/xnetdude/SmartThingsPublic)

[xtreme22886/SmartThings_UniFi-Presence-Sensor](https://github.com/xtreme22886/SmartThings_UniFi-Presence-Sensor)

[xxKeoxx/green-thermostat](https://github.com/xxKeoxx/green-thermostat)

[ygelfand/smartthings](https://github.com/ygelfand/smartthings)

[yifeiy3/DeviceMonitor](https://github.com/yifeiy3/DeviceMonitor)

[yifeiy3/SmartappAnalysis](https://github.com/yifeiy3/SmartappAnalysis)

[yinhaoxiao/Community-SmartApps](https://github.com/yinhaoxiao/Community-SmartApps)

[yosefham/SmartThingsPublic](https://github.com/yosefham/SmartThingsPublic)

[yostinso/AirVisualST](https://github.com/yostinso/AirVisualST)

[yostinso/groovy-parser-ts](https://github.com/yostinso/groovy-parser-ts)

[yracine/DSC-Integration-with-Arduino-Mega-Shield-RS-232](https://github.com/yracine/DSC-Integration-with-Arduino-Mega-Shield-RS-232)

[yracine/device-type-myNext](https://github.com/yracine/device-type-myNext)

[yracine/device-type.myecobee](https://github.com/yracine/device-type.myecobee)

[ytechie/SmartHomeScripts](https://github.com/ytechie/SmartHomeScripts)

[zeande/smartthings](https://github.com/zeande/smartthings)

[zeusmoss/SmartThings.HueColorCycle](https://github.com/zeusmoss/SmartThings.HueColorCycle)

[zeusmoss/SmartThings.SmartIndicatorNightlight](https://github.com/zeusmoss/SmartThings.SmartIndicatorNightlight)

[zhangquan0126/bruntblinds-smarthings](https://github.com/zhangquan0126/bruntblinds-smarthings)

[zhouguiheng/SmartThings](https://github.com/zhouguiheng/SmartThings)

[ziden33/smartthings](https://github.com/ziden33/smartthings)

[zissou1/nibe-uplink-ST](https://github.com/zissou1/nibe-uplink-ST)

[zpriddy/SmartThings](https://github.com/zpriddy/SmartThings)

[zpriddy/SmartThingsHue](https://github.com/zpriddy/SmartThingsHue)

[zpriddy/SmartThingsPubTest](https://github.com/zpriddy/SmartThingsPubTest)

[zpriddy/SmartThings_PyDash](https://github.com/zpriddy/SmartThings_PyDash)

[zpriddy/python_smartthings](https://github.com/zpriddy/python_smartthings)

[zraken/InfinitudeST](https://github.com/zraken/InfinitudeST)

[zub3ra/mailbox](https://github.com/zub3ra/mailbox)

[zzarbi/smartthings](https://github.com/zzarbi/smartthings)

