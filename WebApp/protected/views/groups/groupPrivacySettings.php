<?php 
$this->beginWidget('zii.widgets.jui.CJuiDialog', array(
	    'id'=>'groupPrivacySettingsWindow',
	    // additional javascript options for the dialog plugin
	    'options'=>array(
	        'title'=>Yii::t('general', 'Group Privacy Settings'),
	        'autoOpen'=>false,
	        'modal'=>true, 
			'resizable'=>false,
			'width'=> '400px'      
	    ),
	));
?>

<div>
	<?php $form=$this->beginWidget('CActiveForm', array(
		'id'=>'groupPrivacySettings-form',
		'enableClientValidation'=>true,
		'clientOptions'=> array(
							'validateOnSubmit'=> true,
							'validateOnChange'=>false,
						 ),
	
	)); ?>
	
		<div class="row" style="padding-top:1em">
			<?php echo 'Give permissions to group members by checking/unchecking the fields below:'; ?>
		</div>		
	
		<div class="row" style="padding-top:2em">
			<?php echo $form->checkBox($model,'allowToSeeMyPosition'); ?>
			<?php echo $form->label($model,'allowToSeeMyPosition'); ?>
			<?php echo $form->error($model,'allowToSeeMyPosition'); ?>
		</div>	

		<div class="row buttons" style="padding-top:2em;text-align:center">
			<?php 
				echo CHtml::ajaxSubmitButton('Save', $this->createUrl('groups/setPrivacyRights', array('groupId'=>$groupId)), 
													array(
														'success'=> 'function(result){ 
																		try {
																			var obj = jQuery.parseJSON(result);
																			if (obj.result && obj.result == "1") 
																			{
																				$("#groupPrivacySettingsWindow").dialog("close");
																				$("#messageDialogText").html("Your settings have been saved");
																				$("#messageDialog").dialog("open");																					
																			}
																		}
																		catch (error){
																			$("#groupPrivacySettingsWindow").html(result);
																		}
																	 }',														
														 ),
													null);					
				echo CHtml::htmlButton('Cancel',  
													array(
														'onclick'=> '$("#groupPrivacySettingsWindow").dialog("close"); return false;',
														 ),
													null);					
			?>												
		</div>	
		
	<?php $this->endWidget(); ?>
</div>				

<?php 
	$this->endWidget('zii.widgets.jui.CJuiDialog');
?>