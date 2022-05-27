export default (emit) => {
  const handleChange = (newStatus) => {
    emit('change', newStatus);
  };

  const handleTimeChange = (time) => {
    const timeStatus = {
      startTime: time,
      endTime: time,
    };
    handleChange(timeStatus);
  };

  return {
    handleChange,
    handleTimeChange,
  };
};
