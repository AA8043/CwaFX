# 表单组件

`Form` 会根据 `FormItem` 自动创建输入控件。单行文本、密码和数字输入使用
`MaterialTextField`；提交前会执行必填和要求校验，成功后触发 `FormSubmitEvent`。

```java
Form form = new Form();
FormItem username = new FormItem();
username.setLabelKey("login.username");
username.setRequired(true);
TextLengthRequirement length = new TextLengthRequirement();
length.setMinLength(3);
length.setMaxLength(32);
username.getRequirements().add(length);
form.getItems().add(username);
form.setOnSubmit(event -> {
    String value = (String) event.getValue(0);
});
```

可用的 `ItemType` 为 `TEXT`、`PASSWORD`、`TEXT_AREA`、`COMBO_BOX`、`NUMBER`、
`CHECK_BOX` 和 `DATE`。可选且未填写的文本、下拉和日期值会作为 `null` 提交；数字为
`Double`，复选框为 `Boolean`，日期为 `LocalDate`。

## FXML

`Form` 的默认属性是 `items`，因此可直接嵌套 `FormItem`。所有框架内置提示使用 i18n key，
应用可在自己的 `languages/messages*.properties` 中覆盖同名 key。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import java.lang.String?>
<?import org.a8043.cwaFX.components.form.Form?>
<?import org.a8043.cwaFX.components.form.FormItem?>
<?import org.a8043.cwaFX.components.form.TextLengthRequirement?>

<Form xmlns:fx="http://javafx.com/fxml/1" onSubmit="#submit">
    <FormItem labelKey="login.username" required="true">
        <requirements>
            <TextLengthRequirement minLength="3" maxLength="32" />
        </requirements>
    </FormItem>
    <FormItem labelKey="login.role" type="COMBO_BOX">
        <options>
            <String fx:value="administrator" />
            <String fx:value="viewer" />
        </options>
    </FormItem>
</Form>
```

控制器方法接收 `FormSubmitEvent`，通过 `getValue(index)` 或 `getValues()` 按条目顺序读取结果。
调用 `form.submit()` 可复用内置提交按钮的校验与事件逻辑。
