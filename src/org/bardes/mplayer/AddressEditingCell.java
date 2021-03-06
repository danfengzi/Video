package org.bardes.mplayer;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;

class AddressEditingCell extends TableCell<LayerConfig, Integer>
{
	private TextField textField;

	AddressEditingCell()
	{
	}

	@Override
	public void startEdit()
	{
		if (!isEmpty())
		{
			super.startEdit();
			createTextField();
			setText(null);
			setGraphic(textField);
			textField.selectAll();
		}
	}

	@Override
	public void cancelEdit()
	{
		super.cancelEdit();

		setText(getItem().toString());
		setGraphic(null);
	}

	@Override
	public void updateItem(Integer item, boolean empty)
	{
		super.updateItem(item, empty);

		if (empty)
		{
			setText(item != null ? item.toString() : null);
			setGraphic(null);
		}
		else
		{
			if (isEditing())
			{
				if (textField != null)
				{
					textField.setText(getString());
					// setGraphic(null);
				}
				setText(null);
				setGraphic(textField);
			}
			else
			{
				setText(getString());
				setGraphic(null);
			}
		}
	}

	private void createTextField()
	{
		textField = new TextField(getString());
		textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
		textField.setOnAction((e) -> commitEdit(Integer.parseInt(textField.getText())));
		textField.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
			if (!newValue)
			{
//				System.out.println("Commiting " + textField.getText());
				commitEdit(Integer.getInteger(textField.getText()));
			}
		});
	}

	private String getString()
	{
		return getItem() == null ? "" : getItem().toString();
	}
}