export default (emit) => {
  const handleChange = (newStatus) => {
    emit('change', newStatus);
  };

  const handleTimeChange = (time) => {
    const timeStatus = {
      startTime: time[0],
      endTime: time[1],
    };
    handleChange(timeStatus);
  };

  return {
    handleChange,
    handleTimeChange,
  };
};
