//
//  XCScannerComponent.m
//  AFNetworking
//
//  Created by yangxu on 2018/1/3.
//

#import "XCScannerComponent.h"
#import "SGQRCode.h"
#define OverFloatValue 1.2
@interface XCScannerComponent()<SGQRCodeScanManagerDelegate>
@property (nonatomic, strong) SGQRCodeScanManager *manager;
@property (nonatomic, strong) SGQRCodeScanningView *scanningView;
@end
@implementation XCScannerComponent
{
    UIColor *_borderColor;
    UIColor *_cornerColor;
    CGFloat _cornerWidth;
    CGFloat _backgroudAlpha;
    CornerLocation _cornerLocation;
}

-(instancetype)initWithRef:(NSString *)ref type:(NSString *)type styles:(NSDictionary *)styles attributes:(NSDictionary *)attributes events:(NSArray *)events weexInstance:(WXSDKInstance *)weexInstance{
    self = [super initWithRef:ref type:type styles:styles attributes:attributes events:events weexInstance:weexInstance];
    if (self) {
        if ([[attributes allKeys] containsObject:@"borderColor"])
            _borderColor = [WXConvert UIColor:[attributes valueForKey:@"borderColor"]];
        if ([[attributes allKeys] containsObject:@"cornerColor"])
            _cornerColor = [WXConvert UIColor:[attributes valueForKey:@"cornerColor"]];
        if ([[attributes allKeys] containsObject:@"cornerWidth"]){
            _cornerWidth = [WXConvert CGFloat:[attributes valueForKey:@"cornerWidth"]];
        }else{
            _cornerWidth = OverFloatValue;
        }
        if ([[attributes allKeys] containsObject:@"backgroudAlpha"]){
            _backgroudAlpha = [WXConvert CGFloat:[attributes valueForKey:@"backgroudAlpha"]];
        }else{
            _backgroudAlpha = OverFloatValue;
        }
        
    }
    return self;
}
-(void)viewDidLoad{
    [super viewDidLoad];
    self.view.backgroundColor = [UIColor clearColor];
    [self.view addSubview:self.scanningView];
    [self setupQRCodeScanning];
    [self permissionChecked];
}

-(void)permissionChecked{
    AVCaptureDevice *device = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    if (device) {
        AVAuthorizationStatus status = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo];
        if (status == AVAuthorizationStatusNotDetermined) {
            [AVCaptureDevice requestAccessForMediaType:AVMediaTypeVideo completionHandler:^(BOOL granted) {
                if (granted) {
                    // 用户第一次同意了访问相机权限
                    NSLog(@"用户第一次同意了访问相机权限 - - %@", [NSThread currentThread]);
                    
                } else {
                    // 用户第一次拒绝了访问相机权限
                    NSLog(@"用户第一次拒绝了访问相机权限 - - %@", [NSThread currentThread]);
                }
            }];
        } else if (status == AVAuthorizationStatusAuthorized) { // 用户允许当前应用访问相机
           
        } else if (status == AVAuthorizationStatusDenied) { // 用户拒绝当前应用访问相机
            UIAlertController *alertC = [UIAlertController alertControllerWithTitle:@"温馨提示" message:@"请去-> [设置 - 隐私 - 相机 - SGQRCodeExample] 打开访问开关" preferredStyle:(UIAlertControllerStyleAlert)];
            
            UIAlertAction *alertA = [UIAlertAction actionWithTitle:@"确定" style:(UIAlertActionStyleDefault) handler:^(UIAlertAction * _Nonnull action) {
                
            }];
            [alertC addAction:alertA];
        } else if (status == AVAuthorizationStatusRestricted) {
            NSLog(@"因为系统原因, 无法访问相册");
        }
    } else {
        if (@available(iOS 8.0, *)) {
            UIAlertController *alertC = [UIAlertController alertControllerWithTitle:@"温馨提示" message:@"未检测到您的摄像头" preferredStyle:(UIAlertControllerStyleAlert)];
            UIAlertAction *alertA = [UIAlertAction actionWithTitle:@"确定" style:(UIAlertActionStyleDefault) handler:^(UIAlertAction * _Nonnull action) {
                
            }];
            
            [alertC addAction:alertA];
        } else {
            // Fallback on earlier versions
        }
 
    }
}

-(SGQRCodeScanningView *)scanningView {
    if (!_scanningView) {
        _scanningView = [[SGQRCodeScanningView alloc] initWithFrame:CGRectMake(0, 0, self.view.frame.size.width, self.view.frame.size.height * 0.9)];
        if (_borderColor!=nil) _scanningView.borderColor = _borderColor;
        if (_cornerColor!=nil) _scanningView.cornerColor = _cornerColor;
        if (_backgroudAlpha!=OverFloatValue) _scanningView.backgroundAlpha = _backgroudAlpha;
        if (_cornerWidth!=OverFloatValue) _scanningView.cornerWidth = _cornerWidth;
    }
    return _scanningView;
}

- (void)setupQRCodeScanning {
    self.manager = [SGQRCodeScanManager sharedManager];
    NSArray *arr = @[AVMetadataObjectTypeQRCode, AVMetadataObjectTypeEAN13Code, AVMetadataObjectTypeEAN8Code, AVMetadataObjectTypeCode128Code];
    // AVCaptureSessionPreset1920x1080 推荐使用，对于小型的二维码读取率较高
    [_manager setupSessionPreset:AVCaptureSessionPreset1920x1080 metadataObjectTypes:arr currentController:self.weexInstance.viewController];
    //    [manager cancelSampleBufferDelegate];
    _manager.delegate = self;
}

#pragma mark - - - SGQRCodeScanManagerDelegate
- (void)QRCodeScanManager:(SGQRCodeScanManager *)scanManager didOutputMetadataObjects:(NSArray *)metadataObjects {
    NSLog(@"metadataObjects - - %@", metadataObjects);
    if (metadataObjects != nil && metadataObjects.count > 0) {
        [scanManager palySoundName:@"SGQRCode.bundle/sound.caf"];
        [scanManager stopRunning];
        [scanManager videoPreviewLayerRemoveFromSuperlayer];
        
        AVMetadataMachineReadableCodeObject *obj = metadataObjects[0];
        //self.callBack(@{@"status":@"success",@"result":[obj stringValue]});
    } else {
        //self.callBack(@{@"status":@"error",@"msg":@"未扫描到结果"});
    }
}

@end
